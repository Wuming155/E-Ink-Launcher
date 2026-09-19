package com.wuming.einklauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.wuming.einklauncher.model.AppSortComparator;

/**
 * 设置页面 Fragment。
 */
public class SettingFragment extends Fragment implements View.OnClickListener {

  /** 设置变更回调接口：宿主 Activity 应实现此接口以响应设置变更。 */
  public interface OnSettingChangeListener {
    void onRowNumChanged(int rowNum);
    void onColNumChanged(int colNum);
    void onFontSizeChanged(float size);
    void onAppNameLinesChanged(int lines);
    void onHideDividerChanged(boolean hide);
    void onShowStatusBarChanged(boolean show);
    void onShowCustomIconChanged(boolean show);
    void onSortModeChanged(int mode);
    void onLayoutLockedChanged(boolean locked);
    void onTextBoldChanged(boolean bold);
    void onEnterManageMode();
    void onBackRequested();
    /** 切换布局调整模式（开启进入调整、再点退出） */
    void onToggleLayoutAdjust();
    boolean isLayoutAdjusting();
  }

  private OnSettingChangeListener listener;

  private Spinner colNumSpinner;
  private Spinner rowNumSpinner;
  private Spinner appNameLinesSpinner;
  private Spinner sortModeSpinner;
  private SeekBar fontControl;
  private View rootView;
  private TextView hideDivider;
  private TextView showStatusBar;
  private TextView showCustomIcon;
  private TextView layoutLock;
  private TextView showLockHint;
  private TextView layoutAdjust;
  private TextView textBold;
  private Config config;

  @SuppressWarnings("deprecation")
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof OnSettingChangeListener) {
      listener = (OnSettingChangeListener) activity;
    } else {
      throw new ClassCastException(activity.toString() + " must implement OnSettingChangeListener");
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.activity_setting, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    rootView = getView();
    config = Config.get(getActivity());
    initViews();
    initSpinners();
    initFontControl();
  }

  // =========================================================================
  // 初始化
  // =========================================================================

  private void initViews() {
    rootView.findViewById(R.id.toBack).setOnClickListener(this);
    rootView.findViewById(R.id.rootView).setOnClickListener(this);
    rootView.findViewById(R.id.deleteApp).setOnClickListener(this);
    rootView.findViewById(R.id.btnHideFontControl).setOnClickListener(this);
    rootView.findViewById(R.id.changeFontSize).setOnClickListener(this);
    rootView.findViewById(R.id.helpAbout).setOnClickListener(this);
    rootView.findViewById(R.id.layoutAdjust).setOnClickListener(this);

    showStatusBar = rootView.findViewById(R.id.showStatusBar);
    showCustomIcon = rootView.findViewById(R.id.showCustomIcon);
    layoutLock = rootView.findViewById(R.id.layoutLock);
    showLockHint = rootView.findViewById(R.id.showLockHint);
    layoutAdjust = rootView.findViewById(R.id.layoutAdjust);
    textBold = rootView.findViewById(R.id.textBold);
    hideDivider = rootView.findViewById(R.id.hideDivider);
    fontControl = rootView.findViewById(R.id.font_control);
    colNumSpinner = rootView.findViewById(R.id.col_num_spinner);
    rowNumSpinner = rootView.findViewById(R.id.row_num_spinner);
    appNameLinesSpinner = rootView.findViewById(R.id.appNameLine);
    sortModeSpinner = rootView.findViewById(R.id.sortModeSpinner);

    showStatusBar.setOnClickListener(this);
    hideDivider.setOnClickListener(this);
    showCustomIcon.setOnClickListener(this);
    layoutLock.setOnClickListener(this);
    showLockHint.setOnClickListener(this);
    textBold.setOnClickListener(this);

    // 初始化 UI 状态
    showStatusBar.getPaint().setStrikeThruText(config.isShowStatusBar());
    hideDivider.getPaint().setStrikeThruText(config.isHideDivider());
    hideDivider.setText(config.isHideDivider()
        ? R.string.setting_show_divider : R.string.setting_hide_divider);
    showCustomIcon.getPaint().setStrikeThruText(config.isShowCustomIcon());
    layoutLock.getPaint().setStrikeThruText(config.isLayoutLocked());
    showLockHint.getPaint().setStrikeThruText(config.isShowLockHint());
    textBold.getPaint().setStrikeThruText(config.isTextBold());
    // 布局调整是临时模式（非持久化配置），状态由宿主 Activity 提供
    layoutAdjust.getPaint().setStrikeThruText(listener.isLayoutAdjusting());
    fontControl.setProgress((int) ((config.getFontSize() - 10) * 10));
  }

  private void initSpinners() {
    rowNumSpinner.setSelection(config.getRowNum() - 2, false);
    rowNumSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int rowNum = position + 2;
        config.setRowNum(rowNum);
        listener.onRowNumChanged(rowNum);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    colNumSpinner.setSelection(config.getColNum() - 2, false);
    colNumSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int colNum = position + 2;
        config.setColNum(colNum);
        listener.onColNumChanged(colNum);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    appNameLinesSpinner.setSelection(getAppLineSpinnerSelectPosition(), false);
    appNameLinesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int lines = (position == 3) ? Integer.MAX_VALUE : position;
        config.setAppNameLines(lines);
        listener.onAppNameLinesChanged(lines);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    sortModeSpinner.setSelection(config.getSortMode(), false);
    sortModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (config.isLayoutLocked()) {
          // 锁定状态下拒绝切换排序方式，回退到原选项
          if (position != config.getSortMode()) {
            if (config.isShowLockHint()) {
              Toast.makeText(getActivity(), R.string.layout_locked_toast, Toast.LENGTH_SHORT).show();
            }
            sortModeSpinner.setSelection(config.getSortMode(), false);
          }
          return;
        }
        if (AppSortComparator.modeNeedsUsageStats(position)
            && !AppSortComparator.hasUsageStatsPermission(getActivity())) {
          Toast.makeText(getActivity(), R.string.sort_need_usage_permission, Toast.LENGTH_LONG).show();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
          }
          sortModeSpinner.setSelection(config.getSortMode(), false);
          return;
        }
        config.setSortMode(position);
        listener.onSortModeChanged(position);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  private void initFontControl() {
    fontControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
          float newSize = 10 + progress / 10f;
          config.setFontSize(newSize);
          listener.onFontSizeChanged(newSize);
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });
  }

  private int getAppLineSpinnerSelectPosition() {
    int lines = config.getAppNameLines();
    return (lines <= 2) ? lines : 3;
  }

  // =========================================================================
  // 点击处理
  // =========================================================================

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.toBack || id == R.id.rootView) {
      listener.onBackRequested();
    } else if (id == R.id.deleteApp) {
      handleDeleteApp();
    } else if (id == R.id.showStatusBar) {
      handleToggleStatusBar();
    } else if (id == R.id.helpAbout) {
      AboutDialog.getInstance(getActivity()).show();
    } else if (id == R.id.btnHideFontControl) {
      rootView.findViewById(R.id.menuList).setVisibility(View.VISIBLE);
      rootView.findViewById(R.id.font_control_p).setVisibility(View.GONE);
    } else if (id == R.id.changeFontSize) {
      rootView.findViewById(R.id.menuList).setVisibility(View.GONE);
      rootView.findViewById(R.id.font_control_p).setVisibility(View.VISIBLE);
    } else if (id == R.id.hideDivider) {
      handleToggleDivider();
    } else if (id == R.id.showCustomIcon) {
      handleToggleCustomIcon();
    } else if (id == R.id.layoutLock) {
      handleToggleLayoutLock();
    } else if (id == R.id.showLockHint) {
      handleToggleLockHint();
    } else if (id == R.id.layoutAdjust) {
      handleLayoutAdjust();
    } else if (id == R.id.textBold) {
      handleToggleTextBold();
    }
  }

  private void handleDeleteApp() {
    if (config.isLayoutLocked()) {
      // 布局锁定时关闭管理功能，避免取消隐藏/隐藏应用破坏锁定布局
      if (config.isShowLockHint()) {
        Toast.makeText(getActivity(), R.string.layout_locked_toast, Toast.LENGTH_SHORT).show();
      }
      return;
    }
    listener.onEnterManageMode();
    listener.onBackRequested();
  }

  private void handleToggleStatusBar() {
    boolean newValue = !config.isShowStatusBar();
    config.setShowStatusBar(newValue);
    listener.onShowStatusBarChanged(newValue);
    listener.onBackRequested();
  }

  private void handleToggleDivider() {
    boolean newValue = !config.isHideDivider();
    config.setHideDivider(newValue);
    hideDivider.setText(newValue ? R.string.setting_show_divider : R.string.setting_hide_divider);
    listener.onHideDividerChanged(newValue);
    listener.onBackRequested();
  }

  private void handleToggleCustomIcon() {
    Utils.checkStoragePermission(getActivity(), new Runnable() {
      @Override
      public void run() {
        boolean newValue = !config.isShowCustomIcon();
        config.setShowCustomIcon(newValue);
        listener.onShowCustomIconChanged(newValue);
        listener.onBackRequested();
      }
    });
  }

  /** 布局锁定开关：开启需二次确认，解锁直接生效 */
  private void handleToggleLayoutLock() {
    if (config.isLayoutLocked()) {
      config.setLayoutLocked(false);
      listener.onLayoutLockedChanged(false);
      listener.onBackRequested();
      return;
    }
    new AlertDialog.Builder(getActivity())
        .setTitle(R.string.layout_lock_confirm_title)
        .setMessage(R.string.layout_lock_confirm_message)
        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            config.setLayoutLocked(true);
            listener.onLayoutLockedChanged(true);
            listener.onBackRequested();
          }
        })
        .setNegativeButton(R.string.dialog_cancel, null)
        .show();
  }

  /** 锁定提示开关：仅控制排序方式被拒绝时是否弹 Toast */
  private void handleToggleLockHint() {
    boolean newValue = !config.isShowLockHint();
    config.setShowLockHint(newValue);
    showLockHint.getPaint().setStrikeThruText(newValue);
  }

  private void handleToggleTextBold() {
    boolean newValue = !config.isTextBold();
    config.setTextBold(newValue);
    textBold.getPaint().setStrikeThruText(newValue);
    textBold.invalidate();
    listener.onTextBoldChanged(newValue);
  }

  /** 切换布局调整模式并回到桌面：开启后长按/点击图标交换位置，桌面「完成」退出 */
  private void handleLayoutAdjust() {
    listener.onToggleLayoutAdjust();
    layoutAdjust.getPaint().setStrikeThruText(listener.isLayoutAdjusting());
    listener.onBackRequested();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == Utils.REQUEST_STORAGE_PERMISSION) {
      Utils.onStoragePermissionResult(grantResults);
    }
  }
}
