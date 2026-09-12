package com.wuming.einklauncher.widgets;

import android.content.Context;

import androidx.annotation.Nullable;

import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

public class ObserverFontTextView extends TextView implements Observer {
  public ObserverFontTextView(Context context) {
    super(context);
  }

  public ObserverFontTextView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public ObserverFontTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void update(Observable o, Object arg) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, (Float) arg);
    requestLayout();
  }
}
