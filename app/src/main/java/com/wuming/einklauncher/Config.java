package com.wuming.einklauncher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
  public static final String KEY_CUSTOM_ORDER = "launcherCustomOrder";

  /** 自定义顺序的分隔符（包名不含换行符，可安全用作分隔符） */
  private static final String ORDER_SEPARATOR = "\n";

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

  private static final String PREFS_FILE = "launcherPropertyFile";

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
  private List<String> customOrder;
  private final Set<String> hideApps = new HashSet<>();
  private boolean hideAppsLoaded = false;

  public Config(Context context) {
    this.prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    // 预加载布尔配置
    this.hideDivider = prefs.getBoolean(KEY_HIDE_DIVIDER, DEFAULT_HIDE_DIVIDER);
    this.showStatusBar = prefs.getBoolean(KEY_SHOW_STATUS_BAR, DEFAULT_SHOW_STATUS_BAR);
    this.showCustomIcon = prefs.getBoolean(KEY_SHOW_CUSTOM_ICON, DEFAULT_SHOW_CUSTOM_ICON);
    this.appNameLines = prefs.getInt(KEY_APP_NAME_LINES, DEFAULT_APP_NAME_LINES);
    this.layoutLocked = prefs.getBoolean(KEY_LAYOUT_LOCKED, DEFAULT_LAYOUT_LOCKED);
    this.showLockHint = prefs.getBoolean(KEY_SHOW_LOCK_HINT, DEFAULT_SHOW_LOCK_HINT);
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
    prefs.edit().putStringSet(KEY_HIDE_APPS, hideApps).apply();
  }

  public void removeHideApp(String packageName) {
    ensureHideAppsLoaded();
    hideApps.remove(packageName);
    prefs.edit().putStringSet(KEY_HIDE_APPS, hideApps).apply();
  }

  public void setHideApps(Set<String> hideApps) {
    this.hideApps.clear();
    this.hideApps.addAll(hideApps);
    this.hideAppsLoaded = true;
    prefs.edit().putStringSet(KEY_HIDE_APPS, this.hideApps).apply();
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
    prefs.edit().putBoolean(KEY_LAYOUT_LOCKED, locked).apply();
  }

  /** 锁定状态下切换排序方式被拒绝时，是否弹出提示 */
  public boolean isShowLockHint() {
    return showLockHint;
  }

  public void setShowLockHint(boolean show) {
    this.showLockHint = show;
    prefs.edit().putBoolean(KEY_SHOW_LOCK_HINT, show).apply();
  }

  // ---- 自定义顺序（布局锁定时用于固化图标顺序） ----

  /** 获取自定义顺序（包名有序列表），未设置时返回空列表 */
  public List<String> getCustomOrder() {
    if (customOrder == null) {
      customOrder = new ArrayList<>();
      String raw = prefs.getString(KEY_CUSTOM_ORDER, "");
      if (raw != null && !raw.isEmpty()) {
        for (String pkg : raw.split(ORDER_SEPARATOR)) {
          if (!pkg.isEmpty()) {
            customOrder.add(pkg);
          }
        }
      }
    }
    return customOrder;
  }

  public void setCustomOrder(List<String> order) {
    customOrder = new ArrayList<>(order);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < customOrder.size(); i++) {
      if (i > 0) {
        sb.append(ORDER_SEPARATOR);
      }
      sb.append(customOrder.get(i));
    }
    prefs.edit().putString(KEY_CUSTOM_ORDER, sb.toString()).apply();
  }
}
