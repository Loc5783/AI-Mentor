package com.example.asm_final.utils;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.example.asm_final.R;
import com.google.android.material.button.MaterialButton;

public final class ScreenViews {
  private ScreenViews() {}

  public static int dp(Context c, int n) {
    return Math.round(c.getResources().getDisplayMetrics().density * n);
  }

  public static LinearLayout column(Context c) {
    LinearLayout v = new LinearLayout(c);
    v.setOrientation(LinearLayout.VERTICAL);
    v.setPadding(dp(c, 20), dp(c, 16), dp(c, 20), dp(c, 24));
    v.setBackgroundColor(ContextCompat.getColor(c, R.color.backgroundColor));
    return v;
  }

  public static ScrollView scroll(Context c, View child) {
    ScrollView v = new ScrollView(c);
    v.setFillViewport(true);
    v.addView(child);
    return v;
  }

  public static TextView text(Context c, CharSequence value, int size, boolean bold) {
    TextView v = new TextView(c);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(
        ContextCompat.getColor(c, bold ? R.color.textColorPrimary : R.color.textColorSecondary));
    if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
    v.setPadding(0, dp(c, 6), 0, dp(c, 10));
    return v;
  }

  public static LinearLayout card(Context c) {
    LinearLayout v = column(c);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(ContextCompat.getColor(c, R.color.surfaceColor));
    bg.setCornerRadius(dp(c, 20));
    v.setBackground(bg);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.topMargin = dp(c, 12);
    v.setLayoutParams(p);
    return v;
  }

  public static MaterialButton button(Context c, int label, Runnable action) {
    MaterialButton v = new MaterialButton(c);
    v.setText(label);
    v.setAllCaps(false);
    v.setSingleLine(false);
    v.setMaxLines(3);
    v.setEllipsize(null);
    v.setMinHeight(dp(c, 48));
    v.setOnClickListener(w -> action.run());
    SoftMotion.press(v);
    return v;
  }
}
