package com.example.asm_final.dashboard;

import android.content.Context;
import android.database.Cursor;
import com.example.mentor.data.Timestamps;

/** Shared period aggregation; daily and weekly screens cannot diverge in source/owner filtering. */
public final class QuizProgressReader {
  public static final class Totals {
    public final long xp, attempts;

    Totals(long xp, long attempts) {
      this.xp = xp;
      this.attempts = attempts;
    }
  }

  private QuizProgressReader() {}

  public static Totals load(Context context, int user, long start, long end, long now) {
    if (user <= 0) return new Totals(0, 0);
    QuizModuleStorage.prepare(context);
    long xp = 0, attempts = 0;
    try (com.example.historymodule.databases.SqliteDbHelper helper =
        new com.example.historymodule.databases.SqliteDbHelper(context)) {
      String[] queries = {
        "SELECT timestamp,exp_earned FROM math_quiz_attempts WHERE user_id=? AND total_count>0",
        "SELECT created_at,exp_earned FROM history_quiz_attempts WHERE user_id=? AND total_count>0",
        "SELECT created_at,exp_earned FROM science_quiz_attempts WHERE user_id=? AND"
            + " total_questions>0",
        "SELECT created_at,exp_earned FROM programming_quiz_attempts WHERE user_id=? AND"
            + " total_questions>0"
      };
      for (String sql : queries)
        try (Cursor c =
            helper.getReadableDatabase().rawQuery(sql, new String[] {Integer.toString(user)})) {
          while (c.moveToNext()) {
            long time = Timestamps.parse(c.getString(0));
            if (time >= start && time < end && time <= now) {
              attempts++;
              long amount = Math.max(0, c.getLong(1));
              xp = Long.MAX_VALUE - xp < amount ? Long.MAX_VALUE : xp + amount;
            }
          }
        }
    }
    return new Totals(xp, attempts);
  }
}
