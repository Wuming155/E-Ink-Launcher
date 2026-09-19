package com.wuming.einklauncher;

import android.app.Application;

public class App extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    CrashCapture.getInstance().init(this);
  }


}
