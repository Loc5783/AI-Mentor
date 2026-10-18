package com.example.asm_final.accounts;

import android.content.*;
import com.example.mentor.data.ActiveUser;

/** Local remembered session metadata. Never stores a password or password hash. */
public final class AuthSession {
  private AuthSession() {}

  public static void open(Context c, String username, int id, boolean remember) {
    ActiveUser.set(c, id);
    c.getSharedPreferences("auth_session", 0)
        .edit()
        .putString("username", username)
        .putInt("user_id", id)
        .putBoolean("remember", remember)
        .apply();
  }

  public static String remembered(Context c) {
    android.content.SharedPreferences p = c.getSharedPreferences("auth_session", 0);
    return p.getBoolean("remember", false)
            && p.getInt("user_id", 0) > 0
            && p.getInt("user_id", 0) == ActiveUser.get(c)
        ? p.getString("username", "")
        : "";
  }

  public static void clear(Context c) {
    ActiveUser.clear(c);
    c.getSharedPreferences("auth_session", 0).edit().clear().apply();
  }
}
