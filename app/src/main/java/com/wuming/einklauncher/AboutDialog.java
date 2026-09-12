package com.wuming.einklauncher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutDialog {

  Context context;

  private AboutDialog(Context context) {
    this.context = context;
  }

  public static AboutDialog getInstance(Context context) {
    return new AboutDialog(context);
  }

  private View initLayout() {
    LinearLayout root = new LinearLayout(context);
    int padding = Utils.dp2Px(context, 15);
    root.setPadding(padding, padding, padding, padding);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(0xffffffff);

    TextView appName = new TextView(context);
    appName.setText("E-Ink Launcher");
    appName.setTextSize(30);
    root.addView(appName);

    TextView versionView = new TextView(context);
    versionView.setText(getVersionName());
    versionView.setTextSize(14);
    versionView.setPadding(0, Utils.dp2Px(context, 10), 0, Utils.dp2Px(context, 10));
    root.addView(versionView);

    View line = new View(context);
    line.setBackgroundColor(0xff000000);
    root.addView(line, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dp2Px(context, 1)));

    TextView thanksView = new TextView(context);
    thanksView.setText("Thanks:\nMaciej Haudek");
    thanksView.setTextSize(14);
    thanksView.setPadding(0, Utils.dp2Px(context, 10), 0, 0);
    root.addView(thanksView);

    return root;
  }

  private String getVersionName() {
    try {
      return context.getPackageManager()
          .getPackageInfo(context.getPackageName(), 0).versionName;
    } catch (PackageManager.NameNotFoundException e) {
      return "";
    }
  }

  public void show() {
    new AlertDialog.Builder(context)
        .setView(initLayout())
        .setPositiveButton(R.string.dialog_close, null)
        .show();
  }
}
