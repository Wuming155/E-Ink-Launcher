package com.wuming.einklauncher;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.wuming.einklauncher.model.AdminReceiver;
import com.wuming.einklauncher.model.AppDataCenter;
import com.wuming.einklauncher.model.IconCache;
import com.wuming.einklauncher.model.WifiControl;
import com.wuming.einklauncher.widgets.AppItemBinder;
import com.wuming.einklauncher.widgets.BatteryView;
import com.wuming.einklauncher.widgets.EInkLauncherView;
import com.wuming.einklauncher.widgets.LauncherAdapter;

/**
 * 主界面 Activity - E-Ink 墨水屏桌面启动器。
 */
public class Launcher extends Activity
    implements AppItemBinder.Callback, EInkLauncherView.OnPageChangeListener,
    SettingFragment.OnSettingChangeListener {

  private static final int REQUEST_DEVICE_ADMIN = 10001;
  private static final String STATE_PAGE_INDEX = "pageIndex";
  private static final String STATE_LAYOUT_ADJUSTING = "layoutAdjusting";
  private static final String STATE_ADJUST_SELECTED_PKG = "adjustSelectedPkg";
  private Runnable unregisterBackCallback;

  // ---- Views ----
  private EInkLauncherView launcherView;
  private TextView pageStatus;
  private BatteryView batteryProgress;
  private TextView batteryPercent;
  private TextView batteryStatus;
  private TextView textClock;

  // ---- Data ----
  private AppDataCenter dataCenter;
  private Config config;
  private Calendar calendar;
  private boolean isChina = true;
  private IconCache iconCache;
  private LauncherAdapter adapter;
  private AppItemBinder binder;
  private boolean isSystemApp = false;
  /** 布局调整模式下当前选中的应用包名 */
  private String adjustSelectedPkg;

  // ---- Device Admin ----
  private DevicePolicyManager policyManager;

  // ---- Receivers ----
  private boolean batteryRegistered;
  private boolean timeRegistered;
  private boolean usbRegistered;

  private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      updateTimeShow();
    }
  };

  private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      handleBatteryChanged(intent);
    }
  };

  private final BroadcastReceiver appChangeReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      iconCache.clearAppCache();
      dataCenter.refreshAppList(binder.isDelete());
    }
  };

  private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
        iconCache.markDirty();
        refreshIcons();
      }
    }
  };

  // =========================================================================
  // Lifecycle
  // =========================================================================

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.launcher_activity);

    config = Config.get(this);
    WifiControl.init(this);
    applyStatusBarVisibility();

    isChina = getResources().getConfiguration().locale.getCountry().equals("CN");

    initViews();
    registerStaticReceivers();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      unregisterBackCallback = Api33Back.register(this);
    }
    restoreState(savedInstanceState);
  }

  /**
   * 桌面进程被系统回收后恢复页码与布局调整模式，
   * 避免用户重启桌面丢失当前位置。
   */
  private void restoreState(Bundle saved) {
    if (saved == null) return;
    dataCenter.setPageIndex(saved.getInt(STATE_PAGE_INDEX, 0));
    if (saved.getBoolean(STATE_LAYOUT_ADJUSTING, false)) {
      enterLayoutAdjust();
      String pkg = saved.getString(STATE_ADJUST_SELECTED_PKG);
      if (pkg != null) {
        adjustSelectedPkg = pkg;
        binder.setSelectedPkg(pkg);
        adapter.refreshDisplay();
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(STATE_PAGE_INDEX, dataCenter.getPageIndex());
    outState.putBoolean(STATE_LAYOUT_ADJUSTING, binder.isAdjust());
    outState.putString(STATE_ADJUST_SELECTED_PKG, adjustSelectedPkg);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    // 从其他应用回到桌面：收起设置页、退出未完成的临时模式
    getFragmentManager().popBackStackImmediate();
    if (binder.isAdjust()) {
      exitLayoutAdjust();
    }
    if (binder.isDelete()) {
      binder.setDelete(false);
      dataCenter.refreshAppList();
      config.setHideApps(dataCenter.getHideApps());
    }
    findViewById(R.id.deleteFinish).setVisibility(View.GONE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    registerDynamicReceivers();
    refreshIcons();
  }

  @Override
  protected void onPause() {
    super.onPause();
    unregisterDynamicReceivers();
  }

  @Override
  protected void onDestroy() {
    if (unregisterBackCallback != null) {
      unregisterBackCallback.run();
      unregisterBackCallback = null;
    }
    super.onDestroy();
    unregisterDynamicReceivers();
    try {
      unregisterReceiver(appChangeReceiver);
    } catch (IllegalArgumentException ignored) {
      // 注册失败过（onCreate 提前异常）时这里未注册，忽略
    }
    WifiControl.shutdown();
  }

  // =========================================================================
  // View 初始化
  // =========================================================================

  private void initViews() {
    policyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

    launcherView = findViewById(R.id.mList);
    pageStatus = findViewById(R.id.pageStatus);
    batteryProgress = findViewById(R.id.batteryProgress);
    batteryPercent = findViewById(R.id.batteryPercent);
    batteryStatus = findViewById(R.id.batteryStatus);
    textClock = findViewById(R.id.textClock);

    ImageView settingIcon = findViewById(R.id.toSetting);
    settingIcon.setImageDrawable(
        Utils.tintDrawable(getResources().getDrawable(R.drawable.navibar_icon_settings_highlight),
            ColorStateList.valueOf(0xff000000)));

    // 配置 Binder、Adapter、View
    iconCache = new IconCache();
    binder = new AppItemBinder(getPackageManager());
    binder.setCallback(this);
    binder.setIconCache(iconCache);
    binder.setHideAppPkg(config.getHideApps());
    binder.setCustomLabels(config.getCustomLabels());
    adapter = new LauncherAdapter();
    adapter.setBinder(binder);
    adapter.setFontSize(config.getFontSize());
    adapter.setAppNameLines(config.getAppNameLines());
    adapter.setTextBold(config.isTextBold());
    applyTextBold(config.isTextBold());
    launcherView.setAdapter(adapter);
    launcherView.setOnPageChangeListener(this);

    // 初始化数据中心
    dataCenter = new AppDataCenter(this);
    dataCenter.setSortMode(config.getSortMode());
    dataCenter.setLayoutLocked(config.isLayoutLocked());
    dataCenter.setCustomOrder(config.getCustomOrder());
    dataCenter.setOnOrderChangeListener(new AppDataCenter.OnOrderChangeListener() {
      @Override
      public void onOrderChanged(List<String> newOrder) {
        config.setCustomOrder(newOrder);
      }
    });
    dataCenter.setHideApps(config.getHideApps());
    dataCenter.setPageStatus(pageStatus);
    dataCenter.setAdapter(adapter);

    // 一次性配置网格参数，避免多次重建
    launcherView.configure(config.getColNum(), config.getRowNum(), config.isHideDivider());
    dataCenter.setGridSize(config.getColNum(), config.getRowNum());

    // 翻页按钮
    findViewById(R.id.lastPage).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dataCenter.showLastPage();
      }
    });
    findViewById(R.id.nextPage).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dataCenter.showNextPage();
      }
    });

    // 设置按钮
    findViewById(R.id.toSetting).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new SettingFragment())
            .addToBackStack(null)
            .commit();
      }
    });

    // 完成 / 退出按钮（管理模式与布局调整模式共用）
    findViewById(R.id.deleteFinish).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (binder.isAdjust()) {
          exitLayoutAdjust();
        } else {
          binder.setDelete(false);
          dataCenter.refreshAppList();
          config.setHideApps(dataCenter.getHideApps());
        }
        v.setVisibility(View.GONE);
      }
    });

    // 时间显示
    calendar = Calendar.getInstance();
    updateTimeShow();

    // 检测系统应用
    try {
      isSystemApp = !isUserApp(getPackageManager().getPackageInfo(getPackageName(), 0));
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }

  // =========================================================================
  // SettingFragment.OnSettingChangeListener 实现
  // =========================================================================

  @Override
  public void onRowNumChanged(int rowNum) {
    launcherView.setRowNum(rowNum);
    dataCenter.setRowNum(rowNum);
  }

  @Override
  public void onColNumChanged(int colNum) {
    launcherView.setColNum(colNum);
    dataCenter.setColNum(colNum);
  }

  @Override
  public void onFontSizeChanged(float size) {
    adapter.setFontSize(size);
  }

  @Override
  public void onAppNameLinesChanged(int lines) {
    adapter.setAppNameLines(lines);
  }

  @Override
  public void onHideDividerChanged(boolean hide) {
    launcherView.setHideDivider(hide);
  }

  @Override
  public void onShowStatusBarChanged(boolean show) {
    applyStatusBarVisibility();
  }

  @Override
  public void onShowCustomIconChanged(boolean show) {
    iconCache.markDirty();
    refreshIcons();
  }

  @Override
  public void onEnterManageMode() {
    binder.setDelete(true);
    dataCenter.refreshAppList(true);
    findViewById(R.id.deleteFinish).setVisibility(View.VISIBLE);
  }

  @Override
  public void onSortModeChanged(int mode) {
    if (config.isLayoutLocked()) return;
    dataCenter.reorderAppsByMode(mode);
  }

  @Override
  public void onLayoutLockedChanged(boolean locked) {
    if (locked) {
      // 管理模式下列表包含被隐藏应用与自身，先回到常规列表
      if (binder.isDelete()) {
        binder.setDelete(false);
        findViewById(R.id.deleteFinish).setVisibility(View.GONE);
        dataCenter.refreshAppList();
      }
      // 顺序表由 AppDataCenter 增量维护，这里只同步持久化并锁定
      config.setCustomOrder(dataCenter.getCustomOrder());
      dataCenter.setLayoutLocked(true);
    } else {
      dataCenter.setLayoutLocked(false);
    }
    dataCenter.refreshAppList();
  }

  @Override
  public void onTextBoldChanged(boolean bold) {
    adapter.setTextBold(bold);
    applyTextBold(bold);
    adapter.refreshDisplay();
  }

  private void applyTextBold(boolean bold) {
    if (pageStatus != null) {
      pageStatus.getPaint().setFakeBoldText(bold);
      pageStatus.invalidate();
    }
    if (textClock != null) {
      textClock.getPaint().setFakeBoldText(bold);
      textClock.invalidate();
    }
    if (batteryPercent != null) {
      batteryPercent.getPaint().setFakeBoldText(bold);
      batteryPercent.invalidate();
    }
    if (batteryStatus != null) {
      batteryStatus.getPaint().setFakeBoldText(bold);
      batteryStatus.invalidate();
    }
  }

  // =========================================================================
  // 布局更新
  // =========================================================================

  private void refreshIcons() {
    if (adapter == null || iconCache == null) return;
    iconCache.refreshCustomIcons(getExternalCacheDir() != null, config.isShowCustomIcon());
    adapter.refreshDisplay();
  }

  // =========================================================================
  // AppItemBinder.Callback 实现
  // =========================================================================

  @Override
  public void onItemClick(ResolveInfo info) {
    if (binder.isAdjust()) {
      handleAdjustSelect(info);
      return;
    }

    String pkgName = info.activityInfo.packageName;

    if (AppDataCenter.LOCK_PACKAGE_NAME.equals(pkgName)) {
      lockScreen();
    } else if (AppDataCenter.WIFI_PACKAGE_NAME.equals(pkgName)) {
      WifiControl.onClickWifiItem();
    } else {
      ComponentName comp = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
      Intent intent = new Intent(Intent.ACTION_MAIN);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
      intent.addCategory(Intent.CATEGORY_LAUNCHER);
      intent.setComponent(comp);
      try {
        startActivity(intent);
      } catch (ActivityNotFoundException e) {
        // 应用被卸载但列表未刷新等情况，桌面不应跟着崩
        Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
      } catch (SecurityException e) {
        Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public void onItemLongClick(View anchor, ResolveInfo info) {
    String packageName = info.activityInfo.packageName;

    if (binder.isAdjust()) {
      handleAdjustSelect(info);
    } else if (AppDataCenter.LOCK_PACKAGE_NAME.equals(packageName)) {
      showPowerMenu();
    } else if (AppDataCenter.WIFI_PACKAGE_NAME.equals(packageName)) {
      WifiControl.onLongClickWifiItem();
    } else if (config.isLayoutLocked()) {
      // 布局锁定时关闭管理功能（隐藏/卸载），避免破坏锁定布局
      if (config.isShowLockHint()) {
        Toast.makeText(this, R.string.layout_locked_toast, Toast.LENGTH_SHORT).show();
      }
    } else {
      showAppInfoDialog(info, packageName);
    }
  }

  @Override
  public void onItemDeleteClick(ResolveInfo info) {
    Intent deleteIntent = new Intent(Intent.ACTION_DELETE,
        Uri.parse("package:" + info.activityInfo.packageName));
    try {
      startActivity(deleteIntent);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onItemHideToggle(String packageName, boolean hidden) {
    // 管理模式下的隐藏切换仅更新 UI 状态，"完成" 按钮处理持久化
  }

  // =========================================================================
  // 布局调整模式（交换图标位置）
  // =========================================================================

  /** 选中应用：首次选中高亮，点同一个取消，点另一个交换位置 */
  private void handleAdjustSelect(ResolveInfo info) {
    String pkg = info.activityInfo.packageName;
    if (adjustSelectedPkg == null) {
      adjustSelectedPkg = pkg;
      binder.setSelectedPkg(pkg);
      adapter.refreshDisplay();
    } else if (adjustSelectedPkg.equals(pkg)) {
      adjustSelectedPkg = null;
      binder.setSelectedPkg(null);
      adapter.refreshDisplay();
    } else if (dataCenter.swapApps(adjustSelectedPkg, pkg)) {
      adjustSelectedPkg = null;
      binder.setSelectedPkg(null);
      // swapApps 内部已刷新顺序表并通知持久化，且已重新分页绑定
    }
  }

  private void enterLayoutAdjust() {
    // 与管理模式互斥
    if (binder.isDelete()) {
      binder.setDelete(false);
    }
    // 沿用已保存的顺序表；若为空，随后的刷新会自动登记当前可见应用
    dataCenter.setCustomOrder(config.getCustomOrder());
    adjustSelectedPkg = null;
    binder.setSelectedPkg(null);
    binder.setAdjust(true);
    dataCenter.setLayoutAdjusting(true);
    dataCenter.refreshAppList();
    findViewById(R.id.deleteFinish).setVisibility(View.VISIBLE);
    Toast.makeText(this, R.string.adjust_mode_hint, Toast.LENGTH_LONG).show();
  }

  private void exitLayoutAdjust() {
    adjustSelectedPkg = null;
    binder.setSelectedPkg(null);
    binder.setAdjust(false);
    dataCenter.setLayoutAdjusting(false);
    // 持久化完整顺序表，保证应用安装、更新、清理垃圾或重启后位置都不变
    config.setCustomOrder(dataCenter.getCustomOrder());
    Toast.makeText(this, R.string.layout_adjust_saved, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onToggleLayoutAdjust() {
    if (binder.isAdjust()) {
      exitLayoutAdjust();
      findViewById(R.id.deleteFinish).setVisibility(View.GONE);
    } else {
      enterLayoutAdjust();
    }
  }

  @Override
  public boolean isLayoutAdjusting() {
    return binder.isAdjust();
  }

  // =========================================================================
  // EInkLauncherView.OnPageChangeListener 实现
  // =========================================================================

  @Override
  public void onPageNext() {
    dataCenter.showNextPage();
  }

  @Override
  public void onPagePrev() {
    dataCenter.showLastPage();
  }

  private void showPowerMenu() {
    List<String> items = new ArrayList<>();
    if (isSystemApp) {
      String[] powerArray = getResources().getStringArray(R.array.power_menu);
      Collections.addAll(items, powerArray);
    }
    items.add(getString(R.string.hide_builtin_lock));

    new AlertDialog.Builder(this)
        .setTitle(R.string.item_lockscreen_builtin)
        .setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (isSystemApp && which == 0) {
              Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
              intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              try {
                startActivity(intent);
              } catch (Exception e) {
                Toast.makeText(Launcher.this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
              }
            } else if (isSystemApp && which == 1) {
              try {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                pm.reboot("重启");
              } catch (Exception e) {
                // 非系统签名时 reboot 会抛 SecurityException，提示而不是崩溃
                Toast.makeText(Launcher.this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
              }
            } else {
              Set<String> hideApps = binder.getHideAppPkg();
              hideApps.add(AppDataCenter.LOCK_PACKAGE_NAME);
              config.setHideApps(hideApps);
              dataCenter.refreshAppList();
              Toast.makeText(Launcher.this, R.string.hide_builtin_lock_hint, Toast.LENGTH_LONG).show();
            }
          }
        })
        .setPositiveButton(R.string.dialog_cancel, null)
        .show();
  }

  private void showAppInfoDialog(ResolveInfo info, final String packageName) {
    String customLabel = config.getCustomLabel(packageName);
    CharSequence displayLabel = customLabel != null && !customLabel.isEmpty()
        ? customLabel
        : iconCache.getLabel(packageName, info, getPackageManager());
    new AlertDialog.Builder(this)
        .setIcon(iconCache.getIcon(packageName, info, getPackageManager()))
        .setTitle(displayLabel)
        .setMessage(getString(R.string.dialog_pkg_name, packageName))
        .setPositiveButton(R.string.dialog_rename, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            showRenameDialog(info, packageName);
          }
        })
        .setNeutralButton(R.string.dialog_hide, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Set<String> hideApps = new HashSet<>(binder.getHideAppPkg());
            if (!hideApps.add(packageName)) {
              hideApps.remove(packageName);
            }
            binder.setHideAppPkg(hideApps);
            // 立即同步落盘，避免进程被清理查杀后隐藏状态失效
            config.setHideApps(hideApps);
            dataCenter.refreshAppList();
          }
        })
        .setNegativeButton(R.string.dialog_uninstall, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Intent deleteIntent = new Intent(Intent.ACTION_DELETE,
                Uri.parse("package:" + packageName));
            try {
              startActivity(deleteIntent);
            } catch (ActivityNotFoundException e) {
              Toast.makeText(Launcher.this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
            }
          }
        })
        .show();
  }

  /**
   * 重命名应用：预填当前显示名称，输入为空时恢复原始名称。
   */
  private void showRenameDialog(final ResolveInfo info, final String packageName) {
    String customLabel = config.getCustomLabel(packageName);
    CharSequence current = customLabel != null && !customLabel.isEmpty()
        ? customLabel
        : iconCache.getLabel(packageName, info, getPackageManager());

    final EditText input = new EditText(this);
    input.setText(current);
    input.setSelection(input.getText().length());

    new AlertDialog.Builder(this)
        .setTitle(R.string.dialog_rename)
        .setView(input)
        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            config.setCustomLabel(packageName, input.getText().toString());
            adapter.refreshDisplay();
          }
        })
        .setNegativeButton(R.string.dialog_cancel, null)
        .show();
  }

  // =========================================================================
  // 时间显示
  // =========================================================================

  private void updateTimeShow() {
    if (textClock == null || calendar == null) return;

    boolean is24Hour = DateFormat.is24HourFormat(this);
    calendar.setTimeInMillis(System.currentTimeMillis());

    StringBuilder sb = new StringBuilder("yyyy-MM-dd ");
    if (!is24Hour && isChina) {
      sb.append(Utils.getAMPMCNString(calendar.get(Calendar.HOUR), calendar.get(Calendar.AM_PM)));
    }
    sb.append(is24Hour ? "HH:mm" : "hh:mm");
    if (!is24Hour && !isChina) {
      sb.append(" a");
    }
    sb.append(" EEEE");

    textClock.setText(new SimpleDateFormat(sb.toString(), Locale.getDefault()).format(calendar.getTime()));
  }

  // =========================================================================
  // 电池信息
  // =========================================================================

  private void handleBatteryChanged(Intent intent) {
    int rawLevel = intent.getIntExtra("level", -1);
    int scale = intent.getIntExtra("scale", -1);
    int status = intent.getIntExtra("status", -1);
    int health = intent.getIntExtra("health", -1);

    int level = (rawLevel >= 0 && scale > 0) ? (rawLevel * 100) / scale : -1;
    batteryProgress.setProgress(level);
    if (batteryPercent != null) {
      batteryPercent.setText(level >= 0 ? level + "%" : "--%");
    }
    batteryStatus.setVisibility(View.VISIBLE);

    if (BatteryManager.BATTERY_HEALTH_OVERHEAT == health) {
      batteryStatus.setText(R.string.battery_heat);
      return;
    }

    switch (status) {
      case BatteryManager.BATTERY_STATUS_UNKNOWN:
        batteryStatus.setText(R.string.battery_unknown);
        break;
      case BatteryManager.BATTERY_STATUS_CHARGING:
        batteryStatus.setText(R.string.battery_charging);
        break;
      case BatteryManager.BATTERY_STATUS_DISCHARGING:
      case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
        if (level < 15) {
          batteryStatus.setText(R.string.battery_low);
        } else {
          batteryStatus.setVisibility(View.GONE);
        }
        break;
      case BatteryManager.BATTERY_STATUS_FULL:
        batteryStatus.setText(R.string.battery_full);
        break;
      default:
        batteryStatus.setText(R.string.battery_wtf);
        break;
    }
  }

  // =========================================================================
  // 广播注册/注销
  // =========================================================================

  /** 注册生命周期不变的静态广播 */
  private void registerStaticReceivers() {
    // 应用安装/卸载广播
    IntentFilter appChangeFilter = new IntentFilter();
    appChangeFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
    appChangeFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
    appChangeFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
    appChangeFilter.addDataScheme("package");
    registerCompatReceiver(appChangeReceiver, appChangeFilter);
  }

  /** 注册跟随 onResume/onPause 的动态广播 */
  private void registerDynamicReceivers() {
    if (!batteryRegistered) {
      registerCompatReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
      batteryRegistered = true;
    }
    if (!timeRegistered) {
      registerCompatReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
      timeRegistered = true;
    }
    updateTimeShow();
    if (!usbRegistered) {
      registerUsbReceiver();
    }
  }

  private void unregisterDynamicReceivers() {
    if (batteryRegistered) {
      unregisterReceiver(batteryReceiver);
      batteryRegistered = false;
    }
    if (timeRegistered) {
      unregisterReceiver(timeReceiver);
      timeRegistered = false;
    }
    if (usbRegistered) {
      unregisterReceiver(usbReceiver);
      usbRegistered = false;
    }
  }

  private void registerUsbReceiver() {
    IntentFilter usbFilter = new IntentFilter();
    usbFilter.addAction(Intent.ACTION_UMS_DISCONNECTED);
    usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
    usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
    usbFilter.addDataScheme("file");
    registerCompatReceiver(usbReceiver, usbFilter);
    usbRegistered = true;
  }

  private void registerCompatReceiver(BroadcastReceiver receiver, IntentFilter filter) {
    Utils.registerReceiverCompat(this, receiver, filter);
  }

  // =========================================================================
  // 按键处理
  // =========================================================================

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    // 兼容各类墨水屏阅读器的物理翻页键（不同厂商发的 keycode 不一致）
    switch (keyCode) {
      case KeyEvent.KEYCODE_PAGE_UP:
      case KeyEvent.KEYCODE_BUTTON_L1:
      case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
        dataCenter.showLastPage();
        return true;
      case KeyEvent.KEYCODE_PAGE_DOWN:
      case KeyEvent.KEYCODE_BUTTON_R1:
      case KeyEvent.KEYCODE_MEDIA_NEXT:
      case KeyEvent.KEYCODE_FORWARD:
        dataCenter.showNextPage();
        return true;
      default:
        return super.onKeyUp(keyCode, event);
    }
  }

  @SuppressLint("GestureBackNavigation")
  @Override
  public void onBackPressed() {
    onBackRequested();
  }

  @Override
  public void onBackRequested() {
    getFragmentManager().popBackStackImmediate();
  }

  @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private static final class Api33Back {
    static Runnable register(Launcher activity) {
      android.window.OnBackInvokedDispatcher dispatcher = activity.getOnBackInvokedDispatcher();
      android.window.OnBackInvokedCallback callback = activity::onBackRequested;
      dispatcher.registerOnBackInvokedCallback(
          android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
      return () -> dispatcher.unregisterOnBackInvokedCallback(callback);
    }
  }

  // =========================================================================
  // 锁屏
  // =========================================================================

  public void lockScreen() {
    try {
      if (policyManager.isAdminActive(new ComponentName(this, AdminReceiver.class))) {
        policyManager.lockNow();
      } else {
        requestDeviceAdmin();
      }
    } catch (Exception e) {
      showDeviceAdminDialog();
    }
  }

  private void requestDeviceAdmin() {
    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, AdminReceiver.class));
    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "E-Ink Launcher 获取锁屏权限");
    try {
      startActivity(intent);
    } catch (ActivityNotFoundException e) {
      showDeviceAdminDialog();
    }
  }

  private void showDeviceAdminDialog() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.launch_failed)
        .setMessage(R.string.launch_devicemanager_failed)
        .setPositiveButton(R.string.launch_devicemanager, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            try {
              Intent intent = Intent.parseUri(
                  "intent:#Intent;component=com.android.settings/.DeviceAdminSettings;end",
                  Intent.URI_INTENT_SCHEME);
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(intent);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        })
        .setNegativeButton(R.string.dialog_cancel, null)
        .show();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK && requestCode == REQUEST_DEVICE_ADMIN) {
      try {
        policyManager.lockNow();
      } catch (SecurityException e) {
        showDeviceAdminDialog();
      }
    }
  }

  // =========================================================================
  // 状态栏/系统应用判断/通知栏
  // =========================================================================

  public void applyStatusBarVisibility() {
    int flags = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    if (config.isShowStatusBar()) {
      getWindow().setFlags(flags, flags);
    } else {
      getWindow().clearFlags(flags);
    }
  }

  public boolean isUserApp(PackageInfo pInfo) {
    return (pInfo.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0;
  }
}
