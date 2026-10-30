package com.example.asm_final.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public final class ThemeHelper {
  private static final String SETTINGS_PREFERENCES = "settings";
  private static final String KEY_DARK_MODE = "dark_mode_enabled";
  private static final String KEY_CUSTOM_BACKGROUND = "custom_background_path";

  private ThemeHelper() {
    // Utility class.
  }

  public static String getCustomBackground(Context context) {
    return preferences(context).getString(KEY_CUSTOM_BACKGROUND, null);
  }

  public static void setCustomBackground(Context context, String path) {
    preferences(context).edit().putString(KEY_CUSTOM_BACKGROUND, path).apply();
  }

  public static boolean isDarkMode(Context context) {
    SharedPreferences prefs = preferences(context);
    if (prefs.contains(KEY_DARK_MODE)) {
      return prefs.getBoolean(KEY_DARK_MODE, false);
    }
    int nightMode =
        context.getResources().getConfiguration().uiMode
            & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
    return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
  }

  public static void setDarkMode(Context context, boolean enabled) {
    // Persist before AppCompat recreates the activity so a process restart during the
    // configuration change cannot restore the previous appearance.
    preferences(context).edit().putBoolean(KEY_DARK_MODE, enabled).commit();

    AppCompatDelegate.setDefaultNightMode(
        enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
  }

  public static void applySavedTheme(Context context) {
    SharedPreferences prefs = preferences(context);
    if (prefs.contains(KEY_DARK_MODE)) {
      boolean dark = prefs.getBoolean(KEY_DARK_MODE, false);
      AppCompatDelegate.setDefaultNightMode(
          dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    } else {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
  }

  private static SharedPreferences preferences(Context context) {
    return context
        .getApplicationContext()
        .getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
  }
}
