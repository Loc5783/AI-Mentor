package com.example.historymodule.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the host application's active user id for History-owned data. */
public final class HistoryUserSession {
    private static final String FILE_NAME = "HISTORY_HOST_SESSION";
    private static final String KEY_USER_ID = "history_user_id";

    private HistoryUserSession() {
    }

    public static void setUserId(Context context, int userId) {
        if (context == null) return;
        preferences(context).edit().putInt(KEY_USER_ID, Math.max(0, userId)).apply();
    }

    public static int getUserId(Context context) {
        if (context == null) return 0;
        return Math.max(0, preferences(context).getInt(KEY_USER_ID, 0));
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }
}
