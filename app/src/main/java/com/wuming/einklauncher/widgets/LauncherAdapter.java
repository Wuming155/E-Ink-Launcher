package com.wuming.einklauncher.widgets;

import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import com.wuming.einklauncher.R;
import com.wuming.einklauncher.model.ObservableFloat;

/**
 * {@link EInkLauncherView} 的数据适配器，管理 ViewHolder 池、数据列表和显示参数。
 * <p>
 * 绑定/交互逻辑委派给 {@link AppItemBinder}，自身只关心：
 * <ul>
 *   <li>ViewHolder 的创建与回收</li>
 *   <li>数据列表（当前页应用）的增删</li>
 *   <li>全局显示参数（字体大小、应用名行数）</li>
 * </ul>
 */
public class LauncherAdapter {

  // =========================================================================
  // ViewHolder
  // =========================================================================

  /** 缓存 launcher_item 布局中的所有子 View 引用。 */
  public static class ItemViewHolder {
    public final View itemView;
    public final ImageView appImage;
    public final ObserverFontTextView appName;
    public final View menuContainer;
    public final View menuDelete;
    public final View menuHide;

    ItemViewHolder(View itemView) {
      this.itemView = itemView;
      appImage = itemView.findViewById(R.id.appImage);
      appName = (ObserverFontTextView) itemView.findViewById(R.id.appName);
      menuContainer = ((ViewGroup) itemView).getChildAt(1);
      menuDelete = itemView.findViewById(R.id.menu_delete);
      menuHide = itemView.findViewById(R.id.menu_hide);
    }
  }

  // =========================================================================
  // 字段
  // =========================================================================

  private final ObservableFloat fontSizeObservable = new ObservableFloat();
  private final List<android.content.pm.ResolveInfo> dataList = new ArrayList<>();
  private final List<ItemViewHolder> holders = new ArrayList<>();

  private AppItemBinder binder;
  private EInkLauncherView attachedView;

  private float fontSize = 14;
  private int appNameLines = Integer.MAX_VALUE;
  private boolean textBold = false;

  // =========================================================================
  // View 绑定（包级可见，由 EInkLauncherView 调用）
  // =========================================================================

  void attachView(EInkLauncherView view) {
    this.attachedView = view;
  }

  void detachView() {
    this.attachedView = null;
  }

  // =========================================================================
  // Binder
  // =========================================================================

  /** 设置负责数据绑定和交互的 Binder */
  public void setBinder(AppItemBinder binder) {
    this.binder = binder;
  }

  public AppItemBinder getBinder() {
    return binder;
  }

  // =========================================================================
  // 显示参数
  // =========================================================================

  public void setFontSize(float fontSize) {
    this.fontSize = fontSize;
    fontSizeObservable.set(fontSize);
  }

  public float getFontSize() {
    return fontSize;
  }

  public void setAppNameLines(int lines) {
    this.appNameLines = lines;
    for (ItemViewHolder holder : holders) {
      holder.appName.setMinLines(lines == 2 ? lines : 0);
      holder.appName.setMaxLines(lines);
    }
  }

  public void setTextBold(boolean bold) {
    this.textBold = bold;
    for (ItemViewHolder holder : holders) {
      holder.appName.getPaint().setFakeBoldText(bold);
      holder.appName.invalidate();
    }
  }

  public boolean isTextBold() {
    return textBold;
  }

  /**
   * 统一本页所有应用名的行数。取「用户设置行数、可用高度按当前字号能容纳的行数、
   * 本页最长名称所需行数」三者的最小值，并对所有格子强制相同的
   * minLines/maxLines——每格内容总高一致，同一行图标保持水平对齐，
   * 名称在图标下方换行且不会溢出固定单元格遮挡相邻图标。
   * <p>
   * 由 {@link EInkLauncherView} 在每次布局时调用
   * （availableHeight = 单元格高 − 图标区高）。
   */
  void applyLabelBounds(int availableHeight) {
    if (holders.isEmpty()) return;
    TextView sample = holders.get(0).appName;
    Paint.FontMetrics fm = sample.getPaint().getFontMetrics();
    int lineHeight = (int) Math.ceil(fm.bottom - fm.top);
    if (lineHeight <= 0 || availableHeight <= 0) return;
    int fitLines = Math.max(1, availableHeight / lineHeight);

    // 本页最长名称按当前字号与标签宽度排出来需要多少行
    int neededLines = 1;
    int width = sample.getWidth();
    TextPaint paint = sample.getPaint();
    for (int i = 0; i < holders.size() && i < dataList.size(); i++) {
      CharSequence text = holders.get(i).appName.getText();
      if (text == null || text.length() == 0) continue;
      StaticLayout layout = new StaticLayout(text, paint,
          width > 0 ? width : fitLines * lineHeight,
          Layout.Alignment.ALIGN_CENTER, 1f, 0f, true);
      neededLines = Math.max(neededLines, layout.getLineCount());
    }

    int lines = Math.min(appNameLines, Math.min(fitLines, neededLines));
    for (ItemViewHolder holder : holders) {
      holder.appName.setMinLines(lines);
      holder.appName.setMaxLines(lines);
    }
  }

  // =========================================================================
  // 数据
  // =========================================================================

  /** 设置当前页要显示的应用列表，自动触发重新绑定 */
  public void setAppList(List<android.content.pm.ResolveInfo> appList) {
    dataList.clear();
    dataList.addAll(appList);
    if (attachedView != null) {
      attachedView.rebind();
    }
  }

  /** 仅刷新显示（自定义图标变更后调用） */
  public void refreshDisplay() {
    if (attachedView != null) {
      attachedView.rebind();
    }
  }

  public int getItemCount() {
    return dataList.size();
  }

  List<android.content.pm.ResolveInfo> getData() {
    return dataList;
  }

  // =========================================================================
  // ViewHolder 管理（包级可见，由 EInkLauncherView 调用）
  // =========================================================================

  List<ItemViewHolder> getHolders() {
    return holders;
  }

  int getHolderCount() {
    return holders.size();
  }

  /** 创建一个新的 ViewHolder，注册字体观察者，初始化文字参数 */
  ItemViewHolder createViewHolder(ViewGroup parent) {
    View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.launcher_item, parent, false);
    ItemViewHolder holder = new ItemViewHolder(itemView);
    holders.add(holder);

    fontSizeObservable.addObserver((Observer) holder.appName);
    holder.appName.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    holder.appName.setMinLines(appNameLines == 2 ? appNameLines : 0);
    holder.appName.setMaxLines(appNameLines);
    holder.appName.setEllipsize(TextUtils.TruncateAt.END);
    holder.appName.getPaint().setFakeBoldText(textBold);

    return holder;
  }

  /** 清除所有 ViewHolder 并取消字体观察 */
  void clearHolders() {
    fontSizeObservable.deleteObservers();
    holders.clear();
  }

  // =========================================================================
  // 绑定调度（包级可见，由 EInkLauncherView.rebind 调用）
  // =========================================================================

  /** 委派给 Binder 执行所有 ViewHolder 的数据绑定与状态更新 */
  void bindAll() {
    if (binder != null) {
      binder.bindAll(holders, dataList);
      binder.updateDeleteState(holders, dataList);
    }
  }

  /** 仅更新管理模式 UI 状态 */
  void updateDeleteState() {
    if (binder != null) {
      binder.updateDeleteState(holders, dataList);
    }
  }
}
