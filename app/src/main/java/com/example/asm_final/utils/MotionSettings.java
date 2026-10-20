package com.example.asm_final.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

/** Shared accessibility boundary for all optional app motion, including Android 7 devices. */
public final class MotionSettings {
  private MotionSettings() {}

  public static boolean enabled(Context context) {
    try {
      return (Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled())
          && Settings.Global.getFloat(
                  context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
              > 0f
          && Settings.Global.getFloat(
                  context.getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
              > 0f;
    } catch (SecurityException unavailable) {
      return false;
    }
  }
}
