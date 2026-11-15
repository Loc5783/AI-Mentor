package com.example.mathmodule.calculator;

import android.app.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.example.mathmodule.R;

/** In-app Mathematics bubble; never requests system overlay access. */
public final class MathCalculator {
  private static final java.util.Map<Activity, android.app.Dialog> PANELS =
      new java.util.WeakHashMap<>();
  public static final String TAG = "mentor_math_calculator";

  private static int dp(Context c, int n) {
    return Math.round(n * c.getResources().getDisplayMetrics().density);
  }

  private static SharedPreferences prefs(Context c) {
    return c.getApplicationContext().getSharedPreferences("math_calculator", 0);
  }

  public static void show(Activity a) {
    attach(a);
    open(a);
  }

  public static void attach(Activity a) {
    FrameLayout root = a.findViewById(android.R.id.content);
    if (root == null || root.findViewWithTag(TAG) != null)
      return;
    FrameLayout layer = new FrameLayout(a);
    layer.setTag(TAG);
    root.addView(layer, new FrameLayout.LayoutParams(-1, -1));
    ImageButton bubble = new ImageButton(a);
    bubble.setImageResource(R.drawable.ic_math_calculator_bubble);
    bubble.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
    bubble.setContentDescription(a.getString(R.string.math_calculator_open));
    bubble.setPadding(dp(a, 14), dp(a, 14), dp(a, 14), dp(a, 14));
    GradientDrawable bg = new GradientDrawable();
    bg.setShape(GradientDrawable.OVAL);
    bg.setColor(ContextCompat.getColor(a, R.color.mentor_math_button));
    bubble.setBackground(bg);
    ViewCompat.setElevation(bubble, dp(a, 6));
    layer.addView(bubble, new FrameLayout.LayoutParams(dp(a, 56), dp(a, 56)));
    layer.post(
        () -> {
          bubble.setX(
              Math.max(0, layer.getWidth() - bubble.getWidth()) * prefs(a).getFloat("x", 1));
          bubble.setY(
              Math.max(0, layer.getHeight() - bubble.getHeight() - dp(a, 88))
                  * prefs(a).getFloat("y", 0.72f));
        });
    layer.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> {
          bubble.setX(Math.max(0, Math.min(bubble.getX(), layer.getWidth() - bubble.getWidth())));
          bubble.setY(
              Math.max(
                  0, Math.min(bubble.getY(), layer.getHeight() - bubble.getHeight() - dp(a, 88))));
        });
    bubble.setOnClickListener(v -> open(a));
    bubble.setOnTouchListener(
        new View.OnTouchListener() {
          float x, y, startX, startY;
          boolean dragged;

          public boolean onTouch(View v, MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
              x = e.getRawX();
              y = e.getRawY();
              startX = v.getX();
              startY = v.getY();
              dragged = false;
              return false;
            }
            if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
              if (Math.hypot(e.getRawX() - x, e.getRawY() - y) > dp(a, 8)) dragged = true;
              if (dragged) {
                v.cancelLongPress();
                v.setPressed(false);
                v.setX(
                    Math.max(
                        0, Math.min(startX + e.getRawX() - x, layer.getWidth() - v.getWidth())));
                v.setY(
                    Math.max(
                        0,
                        Math.min(
                            startY + e.getRawY() - y,
                            layer.getHeight() - v.getHeight() - dp(a, 88))));
                return true;
              }
            }
            if (e.getActionMasked() == MotionEvent.ACTION_UP && dragged) {
              float edge =
                  v.getX() < layer.getWidth() / 2f
                      ? 0
                      : Math.max(0, layer.getWidth() - v.getWidth());
              v.animate().x(edge).setDuration(140).start();
              prefs(a)
                  .edit()
                  .putFloat("x", edge == 0 ? 0 : 1)
                  .putFloat(
                      "y", v.getY() / Math.max(1, layer.getHeight() - v.getHeight() - dp(a, 88)))
                  .apply();
              return true;
            }
            return false;
          }
        });
  }

  public static void detach(Activity a) {
    android.app.Dialog dialog = PANELS.remove(a);
    if (dialog != null) dialog.dismiss();
    ViewGroup root = a.findViewById(android.R.id.content);
    if (root == null) return;
    View layer = root.findViewWithTag(TAG);
    if (layer != null) root.removeView(layer);
  }

  public static void open(Activity a) {
    if (a.isFinishing() || a.isDestroyed() || (PANELS.get(a) != null && PANELS.get(a).isShowing()))
      return;
    LinearLayout body = new LinearLayout(a);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(dp(a, 12), dp(a, 8), dp(a, 12), dp(a, 8));
    TextView display = new TextView(a);
    display.setTextSize(24);
    display.setTextColor(com.google.android.material.color.MaterialColors.getColor(
        a, com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
    display.setMinHeight(dp(a, 64));
    display.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    display.setTextIsSelectable(true);
    display.setMaxLines(2);
    display.setEllipsize(android.text.TextUtils.TruncateAt.START);
    display.setTag("mentor_calculator_display");
    display.setPadding(dp(a, 16), dp(a, 4), dp(a, 16), dp(a, 4));
    TextView status = new TextView(a);
    status.setTextColor(com.google.android.material.color.MaterialColors.getColor(
        a, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY));
    status.setPadding(dp(a, 16), dp(a, 4), 0, 0);
    CalculatorSession session = new CalculatorSession(a, display, status);
    String[][] keys = {
      {"DEG", "sin", "cos", "tan", "⌫"},
      {"MC", "asin", "acos", "atan", "C"},
      {"log", "ln", "√", "∛", "^"},
      {"π", "e", "x²", "x!", "1/x"},
      {"nPr", "nCr", "(", ")", "÷"},
      {"7", "8", "9", "×", "%"},
      {"4", "5", "6", "−", "M+"},
      {"1", "2", "3", "+", "MR"},
      {"±", "0", ".", "Ans", "="},
      {"abs", "exp", "10^", "x³", "M−"}
    };
    for (String[] row : keys) {
      LinearLayout line = new LinearLayout(a);
      for (String key : row) {
        com.google.android.material.button.MaterialButton button =
            new com.google.android.material.button.MaterialButton(a);
        button.setText(key.equals("DEG") && !session.degrees ? "RAD" : key);
        button.setTextSize(key.length() > 3 ? 13 : 16);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setCornerRadius(dp(a, 12));
        button.setMinHeight(dp(a, 44));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.setMargins(dp(a, 2), 0, dp(a, 2), 0);
        line.addView(button, p);
        if (key.equals("⌫"))
          button.setContentDescription(a.getString(R.string.math_calculator_backspace));
        button.setOnClickListener(v -> session.press(key, button));
      }
      body.addView(line);
    }
    TextView hint = new TextView(a);
    hint.setText(R.string.math_calculator_hint);
    hint.setTextSize(12);
    hint.setTextColor(com.google.android.material.color.MaterialColors.getColor(
        a, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY));
    body.addView(hint);
    ScrollView scroll = new ScrollView(a);
    scroll.addView(body);
    scroll.setTag("mentor_calculator_keypad");
    LinearLayout panel = new LinearLayout(a);
    panel.setOrientation(LinearLayout.VERTICAL);
    LinearLayout modes = new LinearLayout(a);
    View[] modeViews = {body, null, null};
    com.google.android.material.button.MaterialButton[] modeButtons = new com.google.android.material.button.MaterialButton[3];
    int[] modeNames = {R.string.math_mode_calculate, R.string.math_mode_matrix, R.string.math_mode_statistics};
    for (int index = 0; index < modeButtons.length; index++) {
      final int mode = index;
      com.google.android.material.button.MaterialButton control = new com.google.android.material.button.MaterialButton(a);
      control.setText(modeNames[index]);
      control.setTextSize(12);
      control.setMinWidth(0);
      control.setMinimumWidth(0);
      control.setPadding(0, 0, 0, 0);
      modeButtons[index] = control;
      modes.addView(control, new LinearLayout.LayoutParams(0, -2, 1));
      control.setOnClickListener(v -> {
        if (modeViews[mode] == null)
          modeViews[mode] = mode == 1 ? CalculatorExtras.matrixPanel(a) : CalculatorExtras.statisticsPanel(a);
        scroll.removeAllViews();
        scroll.addView(modeViews[mode]);
        status.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
        display.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < modeButtons.length; i++) modeButtons[i].setAlpha(i == mode ? 1f : 0.6f);
        scroll.post(() -> scroll.scrollTo(0, 0));
      });
    }
    modeButtons[1].setAlpha(0.6f);
    modeButtons[2].setAlpha(0.6f);
    panel.addView(modes, new LinearLayout.LayoutParams(-1, -2));
    panel.addView(status, new LinearLayout.LayoutParams(-1, -2));
    panel.addView(display, new LinearLayout.LayoutParams(-1, -2));
    panel.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
    androidx.appcompat.app.AlertDialog dialog =
        new androidx.appcompat.app.AlertDialog.Builder(a)
            .setTitle(R.string.math_calculator_title)
            .setView(panel)
            .setNegativeButton(R.string.math_calculator_collapse, null)
            .create();
    PANELS.put(a, dialog);
    dialog.setOnDismissListener(d -> PANELS.remove(a));
    dialog.show();
    Window window = dialog.getWindow();
    if (window != null) {
      int width =
          Math.min(dp(a, 360), a.getResources().getDisplayMetrics().widthPixels - dp(a, 24));
      window.setLayout(width, -2);
      window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
      int available = a.getResources().getDisplayMetrics().heightPixels - dp(a, 260);
      scroll.setLayoutParams(
          new LinearLayout.LayoutParams(-1, Math.min(dp(a, 480), Math.max(dp(a, 72), available))));
      // Large text can stack dialog actions. Keep the keypad scrollable above both actions.
      View decor = window.getDecorView();
      decor
          .getViewTreeObserver()
          .addOnGlobalLayoutListener(
              new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                  if (scroll.getHeight() <= 0) return;
                  decor.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                  int[] origin = new int[2];
                  decor.getLocationOnScreen(origin);
                  int bottom = origin[1] + decor.getHeight() - dp(a, 12);
                  int overflow = 0;
                  for (int action : new int[] {android.content.DialogInterface.BUTTON_NEGATIVE}) {
                    Button button = dialog.getButton(action);
                    if (button != null) {
                      int[] position = new int[2];
                      button.getLocationOnScreen(position);
                      overflow = Math.max(overflow, position[1] + button.getHeight() - bottom);
                    }
                  }
                  if (overflow > 0)
                    scroll.setLayoutParams(
                        new LinearLayout.LayoutParams(
                            -1, Math.max(dp(a, 48), scroll.getHeight() - overflow)));
                }
              });
    }
  }

  private static final class CalculatorSession {
    final Activity activity;
    final TextView display;
    final TextView status;
    final StringBuilder expression;
    boolean degrees;
    boolean justCalculated;
    double answer;
    double memory;

    CalculatorSession(Activity activity, TextView display, TextView status) {
      this.activity = activity;
      this.display = display;
      this.status = status;
      SharedPreferences saved = prefs(activity);
      expression = new StringBuilder(saved.getString("expression", ""));
      justCalculated = saved.getBoolean("result", false);
      degrees = saved.getBoolean("degrees", true);
      answer = storedNumber(saved.getString("answer", "0"));
      memory = storedNumber(saved.getString("memory", "0"));
      render();
    }

    private static double storedNumber(String value) {
      try {
        double parsed = Double.parseDouble(value);
        return Double.isFinite(parsed) ? parsed : 0;
      }
      catch (RuntimeException invalid) { return 0; }
    }

    private String evaluate(String value) {
      return CalculatorEngine.calculate(value, degrees, answer, memory);
    }

    private void replaceWithResult(String value) {
      String result = evaluate(value);
      expression.setLength(0);
      expression.append(result);
      answer = Double.parseDouble(result);
      justCalculated = true;
    }

    private void append(String value) {
      if (expression.length() + value.length() <= 160) expression.append(value);
    }

    private void render() {
      display.setText(expression.length() == 0 ? "0" : expression.toString());
      status.setText((degrees ? "DEG" : "RAD") + (memory == 0 ? "" : "  ·  M=" + CalculatorEngine.format(memory)));
      prefs(activity).edit().putString("expression", expression.toString())
          .putBoolean("result", justCalculated).putBoolean("degrees", degrees)
          .putString("answer", Double.toString(answer)).putString("memory", Double.toString(memory)).apply();
    }

    void press(String key, com.google.android.material.button.MaterialButton button) {
      try {
        if (key.equals("DEG")) {
          degrees = !degrees;
          button.setText(degrees ? "DEG" : "RAD");
          render();
          return;
        }
        if (key.equals("MC")) {
          memory = 0;
          render();
          return;
        }
        if (key.equals("M+") || key.equals("M−")) {
          double value = Double.parseDouble(evaluate(expression.toString()));
          double updated = memory + (key.equals("M+") ? value : -value);
          if (!Double.isFinite(updated)) throw new IllegalArgumentException("Memory overflow");
          memory = updated;
          render();
          return;
        }
        boolean startsNew = Character.isDigit(key.charAt(0)) || key.equals(".") || key.equals("(")
            || key.equals("π") || key.equals("e") || key.equals("Ans") || key.equals("MR")
            || key.equals("abs") || key.equals("exp") || key.equals("10^")
            || key.equals("sin") || key.equals("cos") || key.equals("tan")
            || key.equals("asin") || key.equals("acos") || key.equals("atan")
            || key.equals("log") || key.equals("ln");
        if (justCalculated && startsNew) expression.setLength(0);
        justCalculated = false;
        switch (key) {
          case "C": expression.setLength(0); break;
          case "⌫":
            if (expression.length() > 0) expression.deleteCharAt(expression.length() - 1);
            break;
          case "±":
            if (expression.length() == 0) append("-");
            else {
              String previous = expression.toString();
              expression.setLength(0);
              append("-(" + previous + ")");
            }
            break;
          case "√":
          case "∛":
            String root = key.equals("√") ? "sqrt" : "cbrt";
            boolean complete = expression.length() > 0;
            if (complete) try { evaluate(expression.toString()); }
            catch (IllegalArgumentException incomplete) { complete = false; }
            if (complete) replaceWithResult(root + "(" + expression + ")");
            else append(root + "(");
            break;
          case "x²": replaceWithResult("(" + expression + ")^2"); break;
          case "x³": replaceWithResult("(" + expression + ")^3"); break;
          case "x!": replaceWithResult("(" + expression + ")!"); break;
          case "1/x": replaceWithResult("1/(" + expression + ")"); break;
          case "=": replaceWithResult(expression.toString()); break;
          case "MR": append("M"); break;
          case "10^": append("10^("); break;
          case "sin": case "cos": case "tan": case "asin": case "acos": case "atan":
          case "log": case "ln": case "abs": case "exp": append(key + "("); break;
          default: append(key);
        }
        render();
      } catch (IllegalArgumentException error) {
        display.setText(R.string.math_calculator_error);
      }
    }
  }
}
