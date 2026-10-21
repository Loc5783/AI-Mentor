package com.example.asm_final.accounts;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.mentor.data.ModuleDatabaseHelper;
import java.util.*;

/**
 * Explicit identity mapping; neither email nor nickname is an account identifier across servers.
 */
public final class RemoteAccountLinks extends ModuleDatabaseHelper {
  public RemoteAccountLinks(Context context) {
    super(context, "remote_accounts", null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE remote_account_links(endpoint TEXT NOT NULL,remote_id TEXT NOT NULL,"
            + "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
            + "PRIMARY KEY(endpoint,remote_id),UNIQUE(endpoint,user_id))");
  }

  public int owner(String endpoint, String remoteId) {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT user_id FROM remote_account_links WHERE endpoint=? AND remote_id=?",
                new String[] {endpoint, remoteId})) {
      return c.moveToFirst() ? c.getInt(0) : 0;
    }
  }

  public boolean bound(int owner, String endpoint) {
    return !remoteId(owner, endpoint).isEmpty();
  }

  public String remoteId(int owner, String endpoint) {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT remote_id FROM remote_account_links WHERE user_id=? AND endpoint=?",
                new String[] {Integer.toString(owner), endpoint})) {
      return c.moveToFirst() ? c.getString(0) : "";
    }
  }

  public void bind(int owner, String endpoint, String remoteId) {
    getWritableDatabase()
        .execSQL(
            "INSERT INTO remote_account_links(endpoint,remote_id,user_id) VALUES(?,?,?)",
            new Object[] {endpoint, remoteId, owner});
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
