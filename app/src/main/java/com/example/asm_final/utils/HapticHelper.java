package com.example.asm_final.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibratorManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.os.VibrationAttributes;

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
    performVibration(view.getContext());
  }

  public static void performHaptic(Context context) {
    if (context == null || !isHapticEnabled(context)) return;
    performVibration(context);
  }

  private static void performVibration(Context context) {
    try {
      Vibrator vibrator;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        VibratorManager manager = (VibratorManager)
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        vibrator = manager == null ? null : manager.getDefaultVibrator();
      } else {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
      }
      if (vibrator == null || !vibrator.hasVibrator()) return;

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect effect;
        try {
          effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK);
        } catch (IllegalArgumentException unsupported) {
          effect = VibrationEffect.createOneShot(32, 150);
        }
        if (Build.VERSION.SDK_INT >= 33) {
          VibrationAttributes attributes = new VibrationAttributes.Builder()
              .setUsage(VibrationAttributes.USAGE_TOUCH)
              .build();
          vibrator.vibrate(effect, attributes);
        } else {
          vibrator.vibrate(effect);
        }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(32, 150));
      } else {
        vibrator.vibrate(32);
      }
    } catch (Exception ignored) {}
  }
}
