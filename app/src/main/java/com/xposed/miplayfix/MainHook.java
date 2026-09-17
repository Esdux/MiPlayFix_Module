package com.xposed.miplayfix;

import android.os.Process;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * MiPlayFix - 小米投屏服务音频延迟修复模块
 * 基于 libxposed API 101.0.0
 */
public class MainHook extends XposedModule {

    private static final String TARGET_PACKAGE = "com.milink.service";
    private static final String TARGET_CLASS = "com.xiaomi.miplay.mylibrary.mirror.MultiMirrorControl";
    private static final String TARGET_METHOD = "setAudioPlayDelayTime";
    private static final int NEW_DELAY = 185000; // 微秒
    private static final int AUDIO_THREAD_PRIORITY = -16;
    private static final String TAG = "MiPlayFix";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log("模块已加载");
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        try {
            // 仅提升当前执行线程到 Android 音频线程优先级。
            // 不修改 OOM_ADJ / cgroup，避免与 MIUI 的进程管理机制冲突。
            boostCurrentThread();
            hookAudioDelayMethod(param);
        } catch (Throwable e) {
            log("注入失败: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void boostCurrentThread() {
        try {
            int before = Process.getThreadPriority(Process.myTid());
            if (before > AUDIO_THREAD_PRIORITY) {
                Process.setThreadPriority(Process.myTid(), AUDIO_THREAD_PRIORITY);
            }
            log("当前音频处理线程优先级: " + before + " -> "
                    + Process.getThreadPriority(Process.myTid()));
        } catch (Throwable e) {
            // 某些系统版本禁止普通进程降低 nice 值；失败时不影响原功能。
            log("线程优先级调整被系统拒绝: " + e.getClass().getSimpleName());
        }
    }

    private void hookAudioDelayMethod(XposedModuleInterface.PackageLoadedParam param) {
        try {
            ClassLoader classLoader = param.getDefaultClassLoader();
            Class<?> targetClass = Class.forName(TARGET_CLASS, false, classLoader);

            java.lang.reflect.Method targetMethod = targetClass.getDeclaredMethod(
                    TARGET_METHOD,
                    long.class,
                    int.class
            );

            hook(targetMethod).intercept(chain -> {
                try {
                    Object[] args = chain.getArgs().toArray();
                    int originalDelay = (int) args[1];
                    args[1] = NEW_DELAY;
                    return chain.proceed(args);
                } catch (Exception e) {
                    return chain.proceed();
                }
            });
        } catch (ClassNotFoundException e) {
            log("未找到目标类: " + TARGET_CLASS);
        } catch (NoSuchMethodException e) {
            log("未找到目标方法: " + TARGET_METHOD);
        }
    }

    private void log(String message) {
        log(Log.INFO, TAG, message);
    }
}
