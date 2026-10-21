package com.example.asm_final.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeHelper {
  private static final String SETTINGS_PREFERENCES = "settings";
  private static final String KEY_DARK_MODE = "dark_mode_enabled";

  private ThemeHelper() {
    // Utility class.
  }

  public static boolean isDarkMode(Context context) {
    return preferences(context).getBoolean(KEY_DARK_MODE, false);
  }

  public static void setDarkMode(Context context, boolean enabled) {
    preferences(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();

    AppCompatDelegate.setDefaultNightMode(
        enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
  }

  public static void applySavedTheme(Context context) {
    AppCompatDelegate.setDefaultNightMode(
        isDarkMode(context) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
  }

  private static SharedPreferences preferences(Context context) {
    return context
        .getApplicationContext()
        .getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
  }
}
