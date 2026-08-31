package com.example.module;

import android.app.ActivityManager;
import android.os.StatFs;
import java.io.File;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainModule implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        
        // 1. 内存同步放大 (总内存和可用内存一起翻倍)
        XposedHelpers.findAndHookMethod(ActivityManager.class, "getMemoryInfo", ActivityManager.MemoryInfo.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ActivityManager.MemoryInfo mi = (ActivityManager.MemoryInfo) param.args[0];
                if (mi != null) {
                    mi.totalMem *= 2; 
                    mi.availMem *= 2; 
                    mi.threshold *= 2; // 顺便把低内存触发线也放大，防止系统误判
                }
            }
        });

        // 统一的 4 倍放大逻辑（无论是总空间、可用空间还是剩余空间，全部同步乘以 4，保证比例完美契合）
        XC_MethodHook multiplier4 = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object res = param.getResult();
                if (res instanceof Long) {
                    param.setResult((long) res * 4);
                } else if (res instanceof Integer) {
                    param.setResult((int) res * 4);
                }
            }
        };

        // 2. 存储空间劫持 - StatFs 系列（块数、总空间、可用空间、剩余空间全部同步 4 倍放大）
        XposedHelpers.findAndHookMethod(StatFs.class, "getBlockCountLong", multiplier4);
        XposedHelpers.findAndHookMethod(StatFs.class, "getAvailableBlocksLong", multiplier4);
        XposedHelpers.findAndHookMethod(StatFs.class, "getFreeBlocksLong", multiplier4);
        XposedHelpers.findAndHookMethod(StatFs.class, "getTotalBytes", multiplier4);
        XposedHelpers.findAndHookMethod(StatFs.class, "getFreeBytes", multiplier4);
        XposedHelpers.findAndHookMethod(StatFs.class, "getAvailableBytes", multiplier4);

        // 3. 存储空间劫持 - File 系列（总空间、空闲空间、可用空间全覆盖）
        XposedHelpers.findAndHookMethod(File.class, "getTotalSpace", multiplier4);
        XposedHelpers.findAndHookMethod(File.class, "getFreeSpace", multiplier4);
        XposedHelpers.findAndHookMethod(File.class, "getUsableSpace", multiplier4);
    }
} 
