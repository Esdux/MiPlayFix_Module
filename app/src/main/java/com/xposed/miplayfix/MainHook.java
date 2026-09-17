package com.xposed.miplayfix;

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
    private static final int NEW_DELAY = 100000; // 微秒
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
            hookAudioDelayMethod(param);
        } catch (Throwable e) {
            log("注入失败: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
