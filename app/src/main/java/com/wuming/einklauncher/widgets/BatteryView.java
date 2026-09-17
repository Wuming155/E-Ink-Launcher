package com.wuming.einklauncher.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * 经典墨水屏水平电池指示 View。
 * 纯黑白高对比度绘制：矩形边框 + 右侧正极端子 + 内部黑色电量填充块。
 */
public class BatteryView extends View {

  private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private int maxProgress = 100;
  private int progress = 0;

  private final RectF bodyRect = new RectF();
  private final RectF tipRect = new RectF();
  private final RectF fillRect = new RectF();

  public BatteryView(Context context) {
    super(context);
    init();
  }

  public BatteryView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public BatteryView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    strokePaint.setStyle(Paint.Style.STROKE);
    strokePaint.setColor(0xff000000);
    fillPaint.setStyle(Paint.Style.FILL);
    fillPaint.setColor(0xff000000);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;

    float strokeWidth = Math.max(1.5f, h / 8f);
    strokePaint.setStrokeWidth(strokeWidth);

    // 右侧端子
    float tipWidth = Math.max(2f, strokeWidth * 1.5f);
    float tipHeight = h * 0.45f;

    // 电池主体（右侧留出端子宽度）
    float halfStroke = strokeWidth / 2f;
    float bodyLeft = halfStroke;
    float bodyTop = halfStroke;
    float bodyRight = w - tipWidth - halfStroke;
    float bodyBottom = h - halfStroke;

    bodyRect.set(bodyLeft, bodyTop, bodyRight, bodyBottom);
    canvas.drawRect(bodyRect, strokePaint);

    // 正极端子
    tipRect.set(bodyRight, (h - tipHeight) / 2f, w - halfStroke, (h + tipHeight) / 2f);
    canvas.drawRect(tipRect, fillPaint);

    // 内部填充（电量比例）
    float innerPadding = strokeWidth + 1f;
    float innerLeft = bodyLeft + innerPadding;
    float innerTop = bodyTop + innerPadding;
    float innerRightMax = bodyRight - innerPadding;
    float innerBottom = bodyBottom - innerPadding;

    if (innerRightMax > innerLeft && innerBottom > innerTop && maxProgress > 0) {
      float percent = progress * 1f / maxProgress;
      float innerRight = innerLeft + (innerRightMax - innerLeft) * percent;
      if (innerRight > innerLeft) {
        fillRect.set(innerLeft, innerTop, innerRight, innerBottom);
        canvas.drawRect(fillRect, fillPaint);
      }
    }
  }

  public void setMaxProgress(int maxProgress) {
    this.maxProgress = Math.max(1, maxProgress);
    progress = Math.min(progress, this.maxProgress);
    invalidate();
  }

  public void setProgress(int progress) {
    this.progress = Math.max(0, Math.min(progress, maxProgress));
    invalidate();
  }
}
