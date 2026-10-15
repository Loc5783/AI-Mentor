package com.example.mathmodule.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.example.mathmodule.MathDashboardActivity;
import com.example.mathmodule.util.MathUserSession;

/** Public entry point used by the host application to open the Mathematics module. */
public final class MathFeature {
    private MathFeature() {
    }

    public static void open(@NonNull Context context) {
        open(context, 0);
    }

    public static void open(@NonNull Context context, int userId) {
        MathUserSession.setUserId(context, Math.max(0, userId));
        Intent intent = new Intent(context, MathDashboardActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }
}
