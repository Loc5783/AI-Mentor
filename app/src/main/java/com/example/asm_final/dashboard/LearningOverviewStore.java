package com.example.asm_final.dashboard;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.mentor.data.ModuleDatabaseHelper;
import java.util.*;
import org.json.JSONObject;

/** Cache identity includes server, remote account and grade: a local ID alone is not enough. */
public final class LearningOverviewStore extends ModuleDatabaseHelper {
  public LearningOverviewStore(Context context) {
    super(context, "learning_overview", null, 1);
  }

  @Override
  protected Map<String, String> legacyTables() {
    return Collections.emptyMap();
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE learning_overview_cache(user_id INTEGER NOT NULL REFERENCES users(id) ON"
            + " DELETE CASCADE,endpoint TEXT NOT NULL,remote_id TEXT NOT NULL,grade INTEGER NOT"
            + " NULL,body TEXT NOT NULL,PRIMARY KEY(user_id,endpoint,remote_id,grade))");
  }

  public JSONObject load(int owner, String endpoint, String remoteId, int grade)
      throws org.json.JSONException {
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT body FROM learning_overview_cache WHERE user_id=? AND endpoint=? AND"
                    + " remote_id=? AND grade=?",
                new String[] {
                  Integer.toString(owner), endpoint, remoteId, Integer.toString(grade)
                })) {
      return c.moveToFirst() ? new JSONObject(c.getString(0)) : null;
    }
  }

  public void save(int owner, String endpoint, String remoteId, int grade, JSONObject value) {
    getWritableDatabase()
        .execSQL(
            "INSERT OR REPLACE INTO learning_overview_cache(user_id,endpoint,remote_id,grade,body)"
                + " VALUES(?,?,?,?,?)",
            new Object[] {owner, endpoint, remoteId, grade, value.toString()});
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
