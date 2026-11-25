package com.xposed.miplayfix

/**
 * 配置键与默认值。Hook 进程与 UI 进程共用同一份键名，通过 libxposed 的
 * RemotePreferences（XF 101）跨进程共享，不落私有目录、不依赖 root。
 */
object Config {
    /** 跨进程配置文件名，Hook 侧 getRemotePreferences(PREF_NAME) 与 UI 侧一致 */
    const val PREF_NAME = "miplayfix_config"

    // ---- 功能总开关：关闭时所有功能内 Hook 都不加载 ----
    const val MASTER = "master_enabled"

    // ---- 音频流转延迟 ----
    const val AUDIO_ENABLED = "audio_enabled"
    /** 微秒。原模块硬编码 250000；系统默认 5GHz=800000 / 2.4GHz=1000000 */
    const val AUDIO_DEFAULT_DELAY = "audio_default_delay"
    /** 每设备覆盖：audio_delay_<deviceMac>，值为微秒 */
    const val AUDIO_DEVICE_PREFIX = "audio_delay_"

    // ---- 连接介质（手机向电脑发起的请求）----
    const val MEDIUM_ENABLED = "medium_enabled"
    /** 投屏 com.xiaomi.mirror:cast（trustLevel=16）*/
    const val MEDIUM_CAST = "medium_cast"
    /** 互传 miLyraShareTransfer（trustLevel=48）*/
    const val MEDIUM_TRANSFER = "medium_transfer"
    /** 剪贴板 / 其它（trustLevel 其它值）*/
    const val MEDIUM_OTHER = "medium_other"

    // ---- 重新连接 ----
    const val RECONNECT_ENABLED = "reconnect_enabled"

    // ---- 介质取值（与 lyra 内部一致）----
    const val MEDIUM_BLE = 2
    const val MEDIUM_P2P = 32          // 0x20
    const val MEDIUM_WIFI_LAN = 80     // 0x50
    const val MEDIUM_WIFI_LAN1 = 128   // 0x80
    const val MEDIUM_ETHERNET = 8192   // 0x2000

    /** 界面上可选的介质，value 是写进 ChannelOptions 的真实数值 */
    val MEDIUM_CHOICES = listOf(
        MEDIUM_WIFI_LAN1 to "WiFi LAN1 (128) — 推荐，走普通局域网",
        MEDIUM_WIFI_LAN to "WiFi LAN (80)",
        MEDIUM_P2P to "P2P / WiFi Direct (32) — 需要网卡支持承载网络",
        MEDIUM_BLE to "BLE 蓝牙 (2)",
    )

    const val DEFAULT_AUDIO_DELAY = 250000
    const val DEFAULT_MEDIUM_CAST = MEDIUM_WIFI_LAN1
    const val DEFAULT_MEDIUM_TRANSFER = MEDIUM_WIFI_LAN1
    const val DEFAULT_MEDIUM_OTHER = MEDIUM_WIFI_LAN1

    fun deviceKey(mac: String?) = AUDIO_DEVICE_PREFIX + (mac ?: "")

    fun mediumLabel(v: Int): String = when (v) {
        MEDIUM_WIFI_LAN1 -> "WiFi LAN1"
        MEDIUM_WIFI_LAN -> "WiFi LAN"
        MEDIUM_P2P -> "P2P"
        MEDIUM_BLE -> "BLE"
        MEDIUM_ETHERNET -> "以太网"
        else -> v.toString()
    }
}
