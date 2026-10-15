package com.brightpath.science.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.brightpath.science.presentation.ScienceMainActivity;
import com.brightpath.science.util.ScienceUserSession;

/** Public entry point for the Science feature. */
public final class ScienceFeature {
    private ScienceFeature() { }

    public static void open(@NonNull Context context) {
        open(context, 0);
    }

    public static void open(@NonNull Context context, int userId) {
        ScienceUserSession.setUserId(context, Math.max(0, userId));
        Intent intent = new Intent(context, ScienceMainActivity.class);
        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
