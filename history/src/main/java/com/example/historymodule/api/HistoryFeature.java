package com.example.historymodule.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.example.historymodule.HistoryDashboardActivity;
import com.example.historymodule.util.HistoryUserSession;

/** Public entry point used by the host app to open the History module. */
public final class HistoryFeature {
    private HistoryFeature() {
    }

    public static void open(@NonNull Context context) {
        open(context, 0);
    }

    public static void open(@NonNull Context context, int userId) {
        HistoryUserSession.setUserId(context, Math.max(0, userId));
        Intent intent = new Intent(context, HistoryDashboardActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }
}
