package com.example.asm_final.utils;

import android.app.LocaleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class LocaleHelper {
  public static final String LANGUAGE_VIETNAMESE = "vi";
  public static final String LANGUAGE_ENGLISH = "en";

  private static final String SETTINGS_PREFERENCES = "settings";
  private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";

  private LocaleHelper() {
    // Utility class.
  }

  public static void setLocale(Context context, String language) {
    String supportedLanguage = normalizeLanguage(language);
    preferences(context).edit().putString(SELECTED_LANGUAGE, supportedLanguage).apply();

    applyLanguage(context, supportedLanguage);
  }

  public static void applySavedLocale(Context context) {
    if (Build.VERSION.SDK_INT >= 33) {
      LocaleManager manager = context.getSystemService(LocaleManager.class);
      if (manager != null && !manager.getApplicationLocales().isEmpty()) {
        String language = normalizeLanguage(manager.getApplicationLocales().get(0).getLanguage());
        preferences(context).edit().putString(SELECTED_LANGUAGE, language).apply();
        return;
      }
    }
    String language = getLanguage(context);
    LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
    String currentLanguage = currentLocales.isEmpty() ? "" : currentLocales.get(0).getLanguage();

    if (!language.equals(currentLanguage)) {
      applyLanguage(context, language);
    }
  }

  public static String getLanguage(Context context) {
    if (Build.VERSION.SDK_INT >= 33) {
      LocaleManager manager = context.getSystemService(LocaleManager.class);
      if (manager != null && !manager.getApplicationLocales().isEmpty()) {
        return normalizeLanguage(manager.getApplicationLocales().get(0).getLanguage());
      }
    }
    String savedLanguage = preferences(context).getString(SELECTED_LANGUAGE, LANGUAGE_VIETNAMESE);
    return normalizeLanguage(savedLanguage);
  }

  public static boolean isEnglish(Context context) {
    return LANGUAGE_ENGLISH.equals(getLanguage(context));
  }

  private static void applyLanguage(Context context, String language) {
    if (Build.VERSION.SDK_INT >= 33) {
      LocaleManager manager = context.getSystemService(LocaleManager.class);
      if (manager != null) {
        manager.setApplicationLocales(LocaleList.forLanguageTags(language));
        return;
      }
    }
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
  }

  private static String normalizeLanguage(String language) {
    return LANGUAGE_ENGLISH.equalsIgnoreCase(language) ? LANGUAGE_ENGLISH : LANGUAGE_VIETNAMESE;
  }

  private static SharedPreferences preferences(Context context) {
    return context
        .getApplicationContext()
        .getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
  }
}
