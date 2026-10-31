package com.example.asm_final.utils;

import android.app.Activity;
import com.example.asm_final.R;

public final class AuthChrome {
  private AuthChrome() {}

  public static void bind(Activity activity) {
    android.widget.ImageView art = activity.findViewById(R.id.auth_brand_art);
    if (art != null) {
      int illustration =
          activity instanceof com.example.asm_final.activities.RegisterActivity
              ? R.drawable.auth_art_register
              : R.drawable.auth_art_login;
      art.setImageResource(illustration);
    }
    ((android.widget.TextView) activity.findViewById(R.id.auth_theme))
        .setText(
            ThemeHelper.isDarkMode(activity)
                ? R.string.auth_theme_light
                : R.string.auth_theme_dark);
    activity
        .findViewById(R.id.auth_theme)
        .setOnClickListener(
            v -> ThemeHelper.setDarkMode(activity, !ThemeHelper.isDarkMode(activity)));
    activity
        .findViewById(R.id.auth_language)
        .setOnClickListener(
            v -> LocaleHelper.setLocale(activity, LocaleHelper.isEnglish(activity) ? "vi" : "en"));
  }

  public static void terms(Activity activity) {
    new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.auth_terms_title)
        .setMessage(R.string.auth_terms_body)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }
}
