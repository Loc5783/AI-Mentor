package com.brightpath.science.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Module-owned user session. The host passes its user id through ScienceFeature.open(...). */
public final class ScienceUserSession {
    private static final String FILE_NAME = "SCIENCE_HOST_SESSION";
    private static final String KEY_USER_ID = "science_user_id";

    private final SharedPreferences preferences;

    public ScienceUserSession(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public int getUserId() {
        return Math.max(0, preferences.getInt(KEY_USER_ID, 0));
    }

    public static void setUserId(Context context, int userId) {
        context.getApplicationContext()
                .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_USER_ID, Math.max(0, userId))
                .apply();
    }
}
