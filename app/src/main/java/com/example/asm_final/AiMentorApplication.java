package com.example.asm_final;

import android.app.Application;

import com.example.asm_final.utils.LocaleHelper;
import com.example.asm_final.utils.ThemeHelper;

/**
 * Applies the saved visual and language preferences before any Activity is created.
 */
public class AiMentorApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applySavedTheme(this);
        LocaleHelper.applySavedLocale(this);
    }
}
