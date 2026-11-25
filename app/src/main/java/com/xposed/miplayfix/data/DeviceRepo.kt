package com.xposed.miplayfix.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * 读取小米妙播留下的设备缓存，用于「按设备区分音频延迟」。
 *
 * 数据源：/data/data/com.milink.service/databases/miplay_database 的 CacheDevice 表。
 * 难点有三条，都是实测出来的：
 *
 * 1. 库属主是 com.milink.service（另一个 uid），本模块读不到，必须走 root 复制。
 * 2. 该库是 **WAL 模式**：只拷主库会读到旧快照。必须连 -wal/-shm 一起拷，
 *    并且打开时用 OPEN_READWRITE —— OPEN_READONLY 打不开 WAL 库（要能写 -shm）。
 *    复制后 chmod 666，否则 root 拷来的文件本模块进程写不了。
 * 3. 手机上**没有 sqlite3 二进制**，所以只能把文件拷出来用 SQLiteDatabase 打开。
 */
object DeviceRepo {

    private const val TAG = "MiPlayFix"
    private const val SRC = "/data/data/com.milink.service/databases"
    private const val DB = "miplay_database"

    data class Device(
        val mac: String,
        val name: String,
        val ip: String,
        val deviceType: Int,
        val lastUse: Long,
    )

    fun load(ctx: Context): List<Device> {
        val dir = File(ctx.cacheDir, "mpdb").apply { mkdirs() }
        if (!copyOut(dir)) {
            Log.w(TAG, "复制数据库失败：su 未授权或路径不存在")
            return emptyList()
        }

        val path = File(dir, DB).absolutePath
        Log.i(TAG, "准备打开 $path")
        var db: SQLiteDatabase? = null
        return try {
            db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE)
            val out = mutableListOf<Device>()
            db.rawQuery(
                "SELECT mac,name,ip,device_type,current_use_time FROM CacheDevice",
                null,
            ).use { c ->
                while (c.moveToNext()) {
                    val mac = c.getString(0) ?: continue
                    out += Device(
                        mac = mac,
                        name = c.getString(1) ?: "",
                        ip = c.getString(2) ?: "",
                        deviceType = c.getInt(3),
                        lastUse = c.getLong(4),
                    )
                }
            }
            Log.i(TAG, "读到 ${out.size} 条设备记录")
            out.distinctBy { it.mac }.sortedByDescending { it.lastUse }
        } catch (e: Throwable) {
            Log.w(TAG, "读设备缓存失败: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        } finally {
            runCatching { db?.close() }
        }
    }

    /** root 把库文件复制到自己的 cacheDir，并放开权限 */
    private fun copyOut(dir: File): Boolean {
        val d = dir.absolutePath
        val script = buildString {
            append("mkdir -p '$d' && ")
            for (f in listOf(DB, "$DB-wal", "$DB-shm")) {
                append("cp -f '$SRC/$f' '$d/$f' 2>/dev/null; ")
            }
            append("chmod 666 '$d'/* 2>/dev/null")
        }
        return runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
            val code = p.waitFor()
            Log.i(TAG, "su 复制退出码=$code")
            File(dir, DB).exists() && File(dir, DB).length() > 0
        }.getOrElse {
            Log.w(TAG, "执行 su 失败: ${it.message}")
            false
        }
    }

    /** 重新连接：停掉互联服务让它重新拉起 */
    fun reconnect(): Boolean = runCatching {
        val script = "am force-stop com.milink.service; sleep 1; true"
        Runtime.getRuntime().exec(arrayOf("su", "-c", script)).waitFor() == 0
    }.getOrDefault(false)
}
