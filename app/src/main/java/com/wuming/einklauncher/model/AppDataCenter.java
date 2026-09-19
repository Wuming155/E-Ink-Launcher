package com.wuming.einklauncher.model;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.wuming.einklauncher.R;
import com.wuming.einklauncher.widgets.AppItemBinder;
import com.wuming.einklauncher.widgets.LauncherAdapter;

/**
 * 应用数据管理中心，负责加载应用列表和分页逻辑。
 */
public class AppDataCenter {

  /** 虚拟包名：Wifi 控制入口 */
  public static final String WIFI_PACKAGE_NAME = "E-ink_Launcher.WiFi";
  /** 虚拟包名：一键锁屏入口 */
  public static final String LOCK_PACKAGE_NAME = "E-ink_Launcher.Lock";

  private final Context mContext;
  private final List<ResolveInfo> mApps = new ArrayList<>();
  private int pageIndex = 0;
  private int pageCount = 0;
  private int colNum = 5;
  private int rowNum = 5;
  private LauncherAdapter adapter;
  private AppItemBinder binder;
  private TextView pageStatus;
  private final Set<String> hideApps = new HashSet<>();
  private int sortMode = AppSortComparator.SORT_NAME_ASC;
  private boolean layoutLocked = false;
  /** 布局调整模式：临时按自定义顺序排列，允许交换图标位置 */
  private boolean layoutAdjusting = false;
  private final List<String> customOrder = new ArrayList<>();

  public AppDataCenter(Context context) {
    this.mContext = context;
  }

  // =========================================================================
  // Adapter / Binder 绑定
  // =========================================================================

  public void setAdapter(LauncherAdapter adapter) {
    this.adapter = adapter;
    this.binder = adapter.getBinder();
    if (binder != null) {
      binder.setHideAppPkg(hideApps);
    }
    setPageShow();
  }

  public void setPageStatus(TextView pageStatus) {
    this.pageStatus = pageStatus;
    updatePageStatusView();
  }

  // =========================================================================
  // 隐藏应用管理
  // =========================================================================

  public void setHideApps(Set<String> hideApps) {
    this.hideApps.clear();
    this.hideApps.addAll(hideApps);
    loadApps();
  }

  public Set<String> getHideApps() {
    return hideApps;
  }

  // =========================================================================
  // 列数/行数
  // =========================================================================

  public void setColNum(int colNum) {
    this.colNum = colNum;
    updatePageCount();
    setPageShow();
  }

  public void setRowNum(int rowNum) {
    this.rowNum = rowNum;
    updatePageCount();
    setPageShow();
  }

  /** 批量设置行列数，只触发一次分页更新 */
  public void setGridSize(int colNum, int rowNum) {
    this.colNum = colNum;
    this.rowNum = rowNum;
    updatePageCount();
    setPageShow();
  }

  // =========================================================================
  // 排序
  // =========================================================================

  public void setSortMode(int sortMode) {
    this.sortMode = sortMode;
  }

  public int getSortMode() {
    return sortMode;
  }

  /** 设置布局锁定状态：锁定后忽略 {@link #sortMode}，改用自定义顺序 */
  public void setLayoutLocked(boolean locked) {
    this.layoutLocked = locked;
  }

  public boolean isLayoutLocked() {
    return layoutLocked;
  }

  /** 设置自定义顺序（包名列表） */
  public void setCustomOrder(List<String> order) {
    customOrder.clear();
    if (order != null) {
      customOrder.addAll(order);
    }
  }

  /**
   * 返回当前列表顺序（包名），用于布局锁定时快照固化。
   * 同一包名有多个桌面入口（Activity）时只保留首次出现，保证快照中包名唯一、
   * 锁定后重复项的相对位置稳定不随系统查询顺序漂移。
   */
  public List<String> getAppOrder() {
    List<String> order = new ArrayList<>(mApps.size());
    Set<String> seen = new HashSet<>();
    for (ResolveInfo info : mApps) {
      String pkg = info.activityInfo.packageName;
      if (seen.add(pkg)) {
        order.add(pkg);
      }
    }
    return order;
  }

  // =========================================================================
  // 布局调整（交换图标位置）
  // =========================================================================

  /** 设置布局调整状态：调整期间临时按自定义顺序排列 */
  public void setLayoutAdjusting(boolean adjusting) {
    this.layoutAdjusting = adjusting;
  }

  public boolean isLayoutAdjusting() {
    return layoutAdjusting;
  }

  /**
   * 交换两个应用在当前列表中的位置（按包名查找，含虚拟图标）。
   * 交换成功后同步刷新自定义顺序并重新分页显示。
   *
   * @return 是否交换成功
   */
  public boolean swapApps(String pkgA, String pkgB) {
    if (pkgA == null || pkgB == null || pkgA.equals(pkgB)) return false;
    int indexA = -1;
    int indexB = -1;
    // 同包多入口时取首次出现的位置，与自定义顺序索引的解析保持一致
    for (int i = 0; i < mApps.size(); i++) {
      String pkg = mApps.get(i).activityInfo.packageName;
      if (pkgA.equals(pkg)) {
        if (indexA < 0) indexA = i;
      } else if (pkgB.equals(pkg)) {
        if (indexB < 0) indexB = i;
      }
    }
    if (indexA < 0 || indexB < 0) return false;

    Collections.swap(mApps, indexA, indexB);
    // 以交换后的顺序刷新自定义顺序，保证调整期间及锁定布局后顺序稳定
    customOrder.clear();
    customOrder.addAll(getAppOrder());
    setPageShow();
    return true;
  }

  // =========================================================================
  // 翻页
  // =========================================================================

  public int getPageIndex() {
    return pageIndex;
  }

  /** 恢复页码（进程重建后），越界时钳制到有效范围 */
  public void setPageIndex(int index) {
    pageIndex = Math.max(0, Math.min(index, pageCount));
    setPageShow();
  }

  public void showNextPage() {
    if (pageIndex >= pageCount) return;
    pageIndex++;
    setPageShow();
  }

  public void showLastPage() {
    if (pageIndex <= 0) return;
    pageIndex--;
    setPageShow();
  }

  // =========================================================================
  // 刷新
  // =========================================================================

  public void refreshAppList() {
    refreshAppList(false);
  }

  public void refreshAppList(boolean showAll) {
    if (showAll) {
      loadAllApps();
    } else {
      loadApps();
    }
    setPageShow();
  }

  // =========================================================================
  // 内部加载
  // =========================================================================

  private void loadApps() {
    Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

    if (binder != null) {
      hideApps.clear();
      hideApps.addAll(binder.getHideAppPkg());
    }

    mApps.clear();
    List<ResolveInfo> resolved = queryLaunchableApps(mainIntent);
    for (ResolveInfo resolveInfo : resolved) {
      if ("com.wuming.einklauncher.Launcher".equals(resolveInfo.activityInfo.name)) continue;
      if (!hideApps.contains(resolveInfo.activityInfo.packageName)) {
        mApps.add(resolveInfo);
      }
    }

    if (!hideApps.contains(LOCK_PACKAGE_NAME)) {
      mApps.add(createPowerIcon());
    }
    if (!hideApps.contains(WIFI_PACKAGE_NAME)) {
      mApps.add(createWifiIcon());
    }
    sortApps();
    updatePageCount();
  }

  private void loadAllApps() {
    Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

    mApps.clear();
    mApps.addAll(queryLaunchableApps(mainIntent));
    mApps.add(createPowerIcon());
    mApps.add(createWifiIcon());
    if (binder != null) {
      binder.setHideAppPkg(hideApps);
    }
    sortApps();
    updatePageCount();
  }

  /**
   * 查询可启动应用。PMS 在 Binder 死亡/并发变更时可能抛异常或返回 null，
   * 桌面绝不允许因应用列表查询失败而崩溃，出错时返回空列表保持当前 UI 可用。
   */
  private List<ResolveInfo> queryLaunchableApps(Intent mainIntent) {
    try {
      List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivities(mainIntent, 0);
      return list != null ? list : Collections.<ResolveInfo>emptyList();
    } catch (Exception e) {
      android.util.Log.w("AppDataCenter", "queryIntentActivities failed", e);
      return Collections.emptyList();
    }
  }

  private void setPageShow() {
    int itemCount = colNum * rowNum;
    // 应用数减少（卸载/隐藏）但 updatePageCount 尚未跑过时，防 subList 越界
    int pageStart = Math.min(pageIndex * itemCount, mApps.size());
    int pageEnd = Math.min(pageStart + itemCount, mApps.size());
    adapter.setAppList(mApps.subList(pageStart, pageEnd));
    updatePageStatusView();
  }

  private void updatePageStatusView() {
    if (pageStatus != null) {
      pageStatus.setText(mContext.getString(R.string.page_status, pageIndex + 1, pageCount + 1));
    }
  }

  private void updatePageCount() {
    int itemCount = colNum * rowNum;
    pageCount = mApps.size() / itemCount - (mApps.size() % itemCount == 0 ? 1 : 0);
    pageCount = Math.max(pageCount, 0);
    pageIndex = Math.min(pageIndex, pageCount);
  }

  private void sortApps() {
    int mode = (layoutLocked || layoutAdjusting)
        ? AppSortComparator.SORT_CUSTOM : sortMode;
    try {
      Collections.sort(mApps,
          new AppSortComparator(mContext, mContext.getPackageManager(), mode, customOrder));
    } catch (IllegalArgumentException e) {
      // 比较器契约被破坏（排序数据并发变化）时保持当前顺序，不崩桌面
      android.util.Log.w("AppDataCenter", "sortApps failed, keeping current order", e);
    }
  }

  // =========================================================================
  // 虚拟图标创建
  // =========================================================================

  private ResolveInfo createWifiIcon() {
    ResolveInfo resolveInfo = new ResolveInfo();
    resolveInfo.icon = R.drawable.wifi_on;
    resolveInfo.activityInfo = new ActivityInfo();
    resolveInfo.activityInfo.packageName = WIFI_PACKAGE_NAME;
    return resolveInfo;
  }

  private ResolveInfo createPowerIcon() {
    ResolveInfo resolveInfo = new ResolveInfo();
    resolveInfo.icon = R.drawable.ic_onekeylock;
    resolveInfo.activityInfo = new ActivityInfo();
    resolveInfo.activityInfo.packageName = LOCK_PACKAGE_NAME;
    return resolveInfo;
  }
}
