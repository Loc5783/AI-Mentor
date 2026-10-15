package com.example.mathmodule.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores the host user id for module-owned data and future account separation. */
public final class MathUserSession {
    private static final String FILE_NAME = "MATH_HOST_SESSION";
    private static final String KEY_USER_ID = "math_user_id";

    private MathUserSession() {
    }

    public static void setUserId(Context context, int userId) {
        preferences(context)
                .edit()
                .putInt(KEY_USER_ID, Math.max(0, userId))
                .apply();
    }

    public static int getUserId(Context context) {
        return Math.max(0, preferences(context).getInt(KEY_USER_ID, 0));
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }
}
