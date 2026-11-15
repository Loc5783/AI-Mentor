package com.example.asm_final.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.HapticFeedbackConstants;

public final class HapticHelper {
  private static final String PREF_NAME = "settings";
  private static final String KEY_HAPTIC = "haptic_feedback_enabled";

  private HapticHelper() {}

  public static boolean isHapticEnabled(Context context) {
    return context.getApplicationContext()
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_HAPTIC, true);
  }

  public static void setHapticEnabled(Context context, boolean enabled) {
    context.getApplicationContext()
        .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_HAPTIC, enabled)
        .apply();
  }

  public static void performHaptic(View view) {
    if (view == null || !isHapticEnabled(view.getContext())) return;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    } else {
      performHaptic(view.getContext());
    }
  }

  public static void performHaptic(Context context) {
    if (context == null || !isHapticEnabled(context)) return;
    try {
      Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
      if (v != null && v.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
          v.vibrate(20);
        }
      }
    } catch (Exception ignored) {}
  }
}
