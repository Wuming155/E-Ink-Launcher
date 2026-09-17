package com.wuming.einklauncher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.core.graphics.drawable.DrawableCompat;

import java.text.DecimalFormat;
import java.util.Calendar;

/**
 * 通用工具类。
 */
public class Utils {

  /** 存储权限申请的请求码 */
  public static final int REQUEST_STORAGE_PERMISSION = 10003;

  private static final String[] SIZE_UNITS = {"bytes", "KB", "MB", "GB", "TB"};
  private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("####.00");
  private static final double SIZE_THRESHOLD = 0.8;

  private static final String[] CN_AM_PM = {
      "凌晨", "黎明", "早晨", "上午", "中午", "下午", "晚上", "深夜"
  };

  /** 存储权限授权后待执行的操作 */
  private static Runnable pendingStorageAction;

  private Utils() {
    // 工具类不可实例化
  }

  /**
   * 给 Drawable 着色。
   */
  public static Drawable tintDrawable(Drawable drawable, ColorStateList colors) {
    final Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
    DrawableCompat.setTintList(wrappedDrawable, colors);
    return wrappedDrawable;
  }

  /**
   * 将文件大小转为可读字符串。
   */
  public static String getReadableFileSize(long size) {
    if (size < 1024 * SIZE_THRESHOLD) {
      return size + SIZE_UNITS[0];
    } else if (size < 1024L * 1024 * SIZE_THRESHOLD) {
      return SIZE_FORMAT.format(size / 1024f) + SIZE_UNITS[1];
    } else if (size < 1024L * 1024 * 1024 * SIZE_THRESHOLD) {
      return SIZE_FORMAT.format(size / 1024f / 1024f) + SIZE_UNITS[2];
    } else if (size < 1024L * 1024 * 1024 * 1024 * SIZE_THRESHOLD) {
      return SIZE_FORMAT.format(size / 1024f / 1024f / 1024f) + SIZE_UNITS[3];
    } else {
      return SIZE_FORMAT.format(size / 1024f / 1024f / 1024f / 1024f) + SIZE_UNITS[4];
    }
  }

  /**
   * dp 转 px。
   */
  public static int dp2Px(Context context, float dp) {
    final float scale = context.getResources().getDisplayMetrics().density;
    return (int) (dp * scale + 0.5f);
  }

  /**
   * 获取中文时段描述（凌晨/黎明/早晨/上午/中午/下午/晚上/深夜）。
   */
  public static String getAMPMCNString(int hours, int ampm) {
    if (ampm == Calendar.AM) {
      if (hours < 5) return CN_AM_PM[0];       // 凌晨
      if (hours < 7) return CN_AM_PM[1];        // 黎明
      if (hours < 9) return CN_AM_PM[2];        // 早晨
      if (hours < 12) return CN_AM_PM[3];       // 上午
      return CN_AM_PM[0];
    } else {
      if (hours == 0 || hours == 12) return CN_AM_PM[4];  // 中午
      if (hours < 6) return CN_AM_PM[5];        // 下午
      if (hours <= 9) return CN_AM_PM[6];       // 晚上
      return CN_AM_PM[7];                        // 深夜
    }
  }

  /**
   * 兼容 Android 13+ 的广播注册。
   * API 33 起需要指定 RECEIVER_EXPORTED / RECEIVER_NOT_EXPORTED。
   */
  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  public static void registerReceiverCompat(Context context, BroadcastReceiver receiver,
                                             IntentFilter filter) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
    } else {
      context.registerReceiver(receiver, filter);
    }
  }

  /**
   * 检查存储权限，已授予则直接执行 next；
   * 未授予时先保存 next，授权结果通过 {@link #onStoragePermissionResult} 继续执行。
   */
  @SuppressLint("InlinedApi")
  public static void checkStoragePermission(Activity activity, Runnable next) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        && activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_DENIED) {
      String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
      pendingStorageAction = next;
      activity.requestPermissions(permissions, REQUEST_STORAGE_PERMISSION);
    } else if (next != null) {
      next.run();
    }
  }

  /**
   * 存储权限申请结果处理：由持有 {@link #REQUEST_STORAGE_PERMISSION} 请求码的
   * {@code onRequestPermissionsResult} 调用，权限授予时继续执行挂起的操作。
   */
  public static void onStoragePermissionResult(int[] grantResults) {
    Runnable next = pendingStorageAction;
    pendingStorageAction = null;
    if (next == null) return;
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      next.run();
    }
  }
}
