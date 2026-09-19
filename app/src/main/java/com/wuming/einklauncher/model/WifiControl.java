package com.wuming.einklauncher.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.util.Map;

import com.wuming.einklauncher.R;
import com.wuming.einklauncher.Utils;
import com.wuming.einklauncher.widgets.ObserverFontTextView;
import com.wuming.einklauncher.widgets.RatioImageView;

/**
 * WiFi 状态管理及 UI 绑定。
 * 通过 {@link #init(Context)} 初始化单例，{@link #bind(View, Map)} 绑定视图。
 */
public class WifiControl {

  private static final String TAG = "WifiControl";
  private static final String WIFI_ON_RES_NAME = "E-ink_Launcher.WifiOn";
  private static final String WIFI_OFF_RES_NAME = "E-ink_Launcher.WifiOff";

  private ObserverFontTextView appName;
  private RatioImageView appImage;
  private final WifiStateReceiver wifiStateReceiver;
  private final WifiManager wifiManager;
  private final Context appContext;

  private int showNameRes;
  private int showIconRes;
  private String connectWifiName;
  private Map<String, File> iconReplaceMap;

  private static WifiControl instance;

  /**
   * 初始化单例。Launcher 每次 onCreate 都会调用，重复创建会在
   * application context 上叠加永不注销的接收器，故已存在时直接复用。
   */
  public static void init(Context context) {
    if (instance == null) {
      instance = new WifiControl(context.getApplicationContext());
    }
  }

  /** 随 Launcher onDestroy 释放接收器与实例，进程内无其他持有者 */
  public static void shutdown() {
    if (instance != null) {
      instance.appContext.unregisterReceiver(instance.wifiStateReceiver);
      instance = null;
    }
  }

  private WifiControl(Context context) {
    appContext = context.getApplicationContext();
    wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);

    applyWifiState(wifiManager.getWifiState());

    wifiStateReceiver = new WifiStateReceiver();
    IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    Utils.registerReceiverCompat(appContext, wifiStateReceiver, filter);
  }

  public static void bind(View view, Map<String, File> iconReplaceMap) {
    if (view == null) {
      instance.appImage = null;
      instance.appName = null;
      return;
    }
    instance.iconReplaceMap = iconReplaceMap;
    instance.appName = view.findViewById(R.id.appName);
    instance.appImage = view.findViewById(R.id.appImage);
    instance.updateStatus();
  }

  private void updateStatus() {
    if (appName == null) return;

    appName.setText(appContext.getString(showNameRes, connectWifiName));

    String fileName = showIconRes == R.drawable.wifi_on ? WIFI_ON_RES_NAME : WIFI_OFF_RES_NAME;
    File replaceFile = iconReplaceMap != null ? iconReplaceMap.get(fileName) : null;
    if (replaceFile != null) {
      appImage.setImageURI(Uri.fromFile(replaceFile));
    } else {
      appImage.setImageResource(showIconRes);
    }
  }

  public static void onClickWifiItem() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      try {
        Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
        panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        instance.appContext.startActivity(panelIntent);
        return;
      } catch (Exception e) {
        Log.w(TAG, "Settings.Panel.ACTION_WIFI unavailable, falling back to wifi settings", e);
        onLongClickWifiItem();
        return;
      }
    }
    int state = instance.wifiManager.getWifiState();
    boolean isEnabled = (state == WifiManager.WIFI_STATE_ENABLING || state == WifiManager.WIFI_STATE_ENABLED);
    instance.wifiManager.setWifiEnabled(!isEnabled);
  }

  public static void onLongClickWifiItem() {
    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try {
      instance.appContext.startActivity(intent);
    } catch (Exception e) {
      Log.w(TAG, "Unable to open wifi settings", e);
    }
  }

  private void applyWifiState(int wifiState) {
    switch (wifiState) {
      case WifiManager.WIFI_STATE_DISABLED:
        showNameRes = R.string.wifi_status_off;
        showIconRes = R.drawable.wifi_off;
        break;
      case WifiManager.WIFI_STATE_DISABLING:
        showNameRes = R.string.wifi_status_closing;
        showIconRes = R.drawable.wifi_on;
        break;
      case WifiManager.WIFI_STATE_ENABLING:
        showNameRes = R.string.wifi_status_opening;
        showIconRes = R.drawable.wifi_off;
        break;
      case WifiManager.WIFI_STATE_ENABLED:
        showNameRes = R.string.wifi_status_on;
        showIconRes = R.drawable.wifi_on;
        break;
    }
  }

  private class WifiStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
        applyWifiState(wifiState);
      }

      if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (networkInfo != null) {
          handleNetworkStateChanged(networkInfo);
        }
      }

      updateStatus();
    }
  }

  private void handleNetworkStateChanged(NetworkInfo networkInfo) {
    switch (networkInfo.getState()) {
      case CONNECTED:
        Log.d(TAG, "CONNECTED");
        String wifiName = "";
        if (networkInfo.getExtraInfo() != null) {
          wifiName = networkInfo.getExtraInfo().replace("\"", "");
        }
        if (wifiName.isEmpty() && wifiManager.getConnectionInfo() != null) {
          String ssid = wifiManager.getConnectionInfo().getSSID();
          if (ssid != null) {
            wifiName = ssid.replace("\"", "");
          }
        }
        if ("<unknown ssid>".equalsIgnoreCase(wifiName) || "<unknown>".equalsIgnoreCase(wifiName)) {
          wifiName = "";
        }
        if (!TextUtils.isEmpty(wifiName)) {
          wifiName = "\n" + wifiName;
        }
        showNameRes = R.string.wifi_status_connected;
        connectWifiName = wifiName;
        break;
      case CONNECTING:
        Log.d(TAG, "CONNECTING");
        showNameRes = R.string.wifi_status_connecting;
        break;
      case DISCONNECTED:
        Log.d(TAG, "DISCONNECTED");
        showNameRes = R.string.wifi_status_disconnected;
        break;
      case DISCONNECTING:
        Log.d(TAG, "DISCONNECTING");
        showNameRes = R.string.wifi_status_disconnecting;
        break;
      default:
        break;
    }
  }
}
