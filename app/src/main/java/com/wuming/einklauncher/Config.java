package com.wuming.einklauncher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 应用配置管理类。
 * 统一管理 SharedPreferences 的读写，缓存常用配置值。
 * 偏好键（KEY_*）集中定义于此类。
 */
public class Config {

  // ---- 偏好键常量 ----
  public static final String KEY_COL_NUM = "colNumKey";
  public static final String KEY_ROW_NUM = "rowNumKey";
  public static final String KEY_APP_NAME_LINES = "appNameShowLines";
  public static final String KEY_HIDE_APPS = "hideAppsKey";
  public static final String KEY_FONT_SIZE = "launcherFontSize";
  public static final String KEY_HIDE_DIVIDER = "launcherHideDivider";
  public static final String KEY_SHOW_STATUS_BAR = "launcherShowStatusBar";
  public static final String KEY_SHOW_CUSTOM_ICON = "launcherShowCustomIcon";
  public static final String KEY_SORT_MODE = "launcherSortMode";
  public static final String KEY_LAYOUT_LOCKED = "launcherLayoutLocked";
  public static final String KEY_SHOW_LOCK_HINT = "launcherShowLockHint";
  public static final String KEY_TEXT_BOLD = "launcherTextBold";
  public static final String KEY_CUSTOM_ORDER = "launcherCustomOrder";
  public static final String KEY_CUSTOM_LABELS = "launcherCustomLabels";

  // ---- 默认值 ----
  private static final int DEFAULT_COL_NUM = 5;
  private static final int DEFAULT_ROW_NUM = 5;
  private static final float DEFAULT_FONT_SIZE = 14f;
  private static final int DEFAULT_APP_NAME_LINES = Integer.MAX_VALUE;
  private static final boolean DEFAULT_HIDE_DIVIDER = true;
  private static final boolean DEFAULT_SHOW_STATUS_BAR = true;
  private static final boolean DEFAULT_SHOW_CUSTOM_ICON = false;
  private static final int DEFAULT_SORT_MODE = 0;
  private static final boolean DEFAULT_LAYOUT_LOCKED = false;
  private static final boolean DEFAULT_SHOW_LOCK_HINT = true;
  private static final boolean DEFAULT_TEXT_BOLD = false;

  private static final String PREFS_FILE = "launcherPropertyFile";

  private static Config instance;

  private final SharedPreferences prefs;

  // ---- 缓存字段 ----
  private int colNum = -1;
  private int rowNum = -1;
  private float fontSize = -1;
  private int appNameLines = -1;
  private boolean hideDivider;
  private boolean showStatusBar;
  private boolean showCustomIcon;
  private int sortMode = -1;
  private boolean layoutLocked;
  private boolean showLockHint;
  private boolean textBold;
  private List<String> customOrder;
  private Map<String, String> customLabels;
  private final Set<String> hideApps = new HashSet<>();
  private boolean hideAppsLoaded = false;

  /**
   * 获取进程内共享实例。Launcher 与 SettingFragment 等多方读写同一份配置，
   * 各自 new 实例会持有独立缓存导致互相覆盖，必须统一从这里获取。
   */
  public static Config get(Context context) {
    if (instance == null) {
      instance = new Config(context.getApplicationContext());
    }
    return instance;
  }

  private Config(Context context) {
    this.prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    // 预加载布尔配置
    this.hideDivider = prefs.getBoolean(KEY_HIDE_DIVIDER, DEFAULT_HIDE_DIVIDER);
    this.showStatusBar = prefs.getBoolean(KEY_SHOW_STATUS_BAR, DEFAULT_SHOW_STATUS_BAR);
    this.showCustomIcon = prefs.getBoolean(KEY_SHOW_CUSTOM_ICON, DEFAULT_SHOW_CUSTOM_ICON);
    this.appNameLines = prefs.getInt(KEY_APP_NAME_LINES, DEFAULT_APP_NAME_LINES);
    this.layoutLocked = prefs.getBoolean(KEY_LAYOUT_LOCKED, DEFAULT_LAYOUT_LOCKED);
    this.showLockHint = prefs.getBoolean(KEY_SHOW_LOCK_HINT, DEFAULT_SHOW_LOCK_HINT);
    this.textBold = prefs.getBoolean(KEY_TEXT_BOLD, DEFAULT_TEXT_BOLD);
  }

  // ---- 列数 ----

  public int getColNum() {
    if (colNum == -1) {
      colNum = prefs.getInt(KEY_COL_NUM, DEFAULT_COL_NUM);
    }
    return colNum;
  }

  public void setColNum(int colNum) {
    if (this.colNum == colNum) return;
    this.colNum = colNum;
    prefs.edit().putInt(KEY_COL_NUM, colNum).apply();
  }

  // ---- 行数 ----

  public int getRowNum() {
    if (rowNum == -1) {
      rowNum = prefs.getInt(KEY_ROW_NUM, DEFAULT_ROW_NUM);
    }
    return rowNum;
  }

  public void setRowNum(int rowNum) {
    if (this.rowNum == rowNum) return;
    this.rowNum = rowNum;
    prefs.edit().putInt(KEY_ROW_NUM, rowNum).apply();
  }

  // ---- 隐藏应用 ----

  public void addHideApp(String packageName) {
    ensureHideAppsLoaded();
    hideApps.add(packageName);
    prefs.edit().putStringSet(KEY_HIDE_APPS, hideApps).commit();
  }

  public void removeHideApp(String packageName) {
    ensureHideAppsLoaded();
    hideApps.remove(packageName);
    prefs.edit().putStringSet(KEY_HIDE_APPS, hideApps).commit();
  }

  public void setHideApps(Set<String> hideApps) {
    this.hideApps.clear();
    this.hideApps.addAll(hideApps);
    this.hideAppsLoaded = true;
    prefs.edit().putStringSet(KEY_HIDE_APPS, this.hideApps).commit();
  }

  public Set<String> getHideApps() {
    ensureHideAppsLoaded();
    return hideApps;
  }

  private void ensureHideAppsLoaded() {
    if (!hideAppsLoaded) {
      hideApps.addAll(prefs.getStringSet(KEY_HIDE_APPS, new HashSet<String>()));
      hideAppsLoaded = true;
    }
  }

  // ---- 字体大小 ----

  public float getFontSize() {
    if (fontSize < 0) {
      fontSize = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
    return fontSize;
  }

  public void setFontSize(float fontSize) {
    this.fontSize = fontSize;
    prefs.edit().putFloat(KEY_FONT_SIZE, fontSize).apply();
  }

  // ---- 分隔线 ----

  public boolean isHideDivider() {
    return hideDivider;
  }

  public void setHideDivider(boolean hide) {
    this.hideDivider = hide;
    prefs.edit().putBoolean(KEY_HIDE_DIVIDER, hide).apply();
  }

  // ---- 状态栏 ----

  public boolean isShowStatusBar() {
    return showStatusBar;
  }

  public void setShowStatusBar(boolean show) {
    this.showStatusBar = show;
    prefs.edit().putBoolean(KEY_SHOW_STATUS_BAR, show).apply();
  }

  // ---- 自定义图标 ----

  public boolean isShowCustomIcon() {
    return showCustomIcon;
  }

  public void setShowCustomIcon(boolean show) {
    this.showCustomIcon = show;
    prefs.edit().putBoolean(KEY_SHOW_CUSTOM_ICON, show).apply();
  }

  // ---- 应用名行数 ----

  public int getAppNameLines() {
    return appNameLines;
  }

  public void setAppNameLines(int lines) {
    this.appNameLines = lines;
    prefs.edit().putInt(KEY_APP_NAME_LINES, lines).apply();
  }

  // ---- 排序方式 ----

  public int getSortMode() {
    if (sortMode == -1) {
      sortMode = prefs.getInt(KEY_SORT_MODE, DEFAULT_SORT_MODE);
    }
    return sortMode;
  }

  public void setSortMode(int mode) {
    if (this.sortMode == mode) return;
    this.sortMode = mode;
    prefs.edit().putInt(KEY_SORT_MODE, mode).apply();
  }

  // ---- 布局锁定 ----

  /** 布局是否已锁定（锁定后图标顺序固化，排序方式不可修改） */
  public boolean isLayoutLocked() {
    return layoutLocked;
  }

  public void setLayoutLocked(boolean locked) {
    this.layoutLocked = locked;
    prefs.edit().putBoolean(KEY_LAYOUT_LOCKED, locked).commit();
  }

  /** 锁定状态下切换排序方式被拒绝时，是否弹出提示 */
  public boolean isShowLockHint() {
    return showLockHint;
  }

  public void setShowLockHint(boolean show) {
    this.showLockHint = show;
    prefs.edit().putBoolean(KEY_SHOW_LOCK_HINT, show).apply();
  }

  // ---- 文字加粗 ----

  public boolean isTextBold() {
    return textBold;
  }

  public void setTextBold(boolean bold) {
    if (this.textBold == bold) return;
    this.textBold = bold;
    prefs.edit().putBoolean(KEY_TEXT_BOLD, bold).apply();
  }

  // ---- 自定义顺序（桌面图标顺序的持久化登记表） ----

  /** 获取自定义顺序（包名有序列表），未设置时返回空列表 */
  public List<String> getCustomOrder() {
    if (customOrder == null) {
      customOrder = ConfigCodec.decodeOrder(prefs.getString(KEY_CUSTOM_ORDER, ""));
    }
    return customOrder;
  }

  /**
   * 保存桌面图标顺序。使用 commit 同步落盘：
   * 顺序一旦变化就立即写盘，即使随后进程被"清理垃圾"强行杀死也不会丢失。
   */
  public void setCustomOrder(List<String> order) {
    customOrder = new ArrayList<>(order);
    prefs.edit().putString(KEY_CUSTOM_ORDER, ConfigCodec.encodeOrder(customOrder)).commit();
  }

  // ---- 自定义应用名称 ----

  /** 获取自定义名称映射（包名 → 名称），未设置时返回空 Map */
  public Map<String, String> getCustomLabels() {
    if (customLabels == null) {
      customLabels = ConfigCodec.decodeLabels(prefs.getString(KEY_CUSTOM_LABELS, ""));
    }
    return customLabels;
  }

  /** 获取指定包名的自定义名称，未设置时返回 null */
  public String getCustomLabel(String packageName) {
    return getCustomLabels().get(packageName);
  }

  /**
   * 设置应用的自定义名称，空名称表示清除、恢复显示原始名称。
   * 存储为 JSON 格式，名称可含任意字符（换行等特殊字符不再需要转义）。
   */
  public void setCustomLabel(String packageName, String label) {
    Map<String, String> labels = getCustomLabels();
    if (label == null || label.trim().isEmpty()) {
      if (labels.remove(packageName) == null) {
        return;
      }
    } else {
      labels.put(packageName, label.trim());
    }
    prefs.edit().putString(KEY_CUSTOM_LABELS, ConfigCodec.encodeLabels(labels)).apply();
  }
}
