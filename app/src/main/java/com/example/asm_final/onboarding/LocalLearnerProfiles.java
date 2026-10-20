package com.example.asm_final.onboarding;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import com.example.asm_final.activities.database.DatabaseHelper;

public final class LocalLearnerProfiles implements LearnerProfileRepository {
  @Override
  public void setGrade(int userId, int grade) {
    if (userId <= 0 || grade < 10 || grade > 12)
      throw new IllegalArgumentException("Invalid learner grade");
    try (DatabaseHelper h = new DatabaseHelper(context)) {
      ContentValues values = new ContentValues();
      values.put("grade", grade);
      if (h.getWritableDatabase()
              .update(
                  "learner_profiles", values, "user_id=?", new String[] {Integer.toString(userId)})
          != 1) throw new SQLiteException("Missing learner profile");
    }
  }

  private final Context context;

  public LocalLearnerProfiles(Context c) {
    context = c.getApplicationContext();
  }

  public LearnerProfile load(int id) {
    try (DatabaseHelper helper = new DatabaseHelper(context);
        Cursor c =
            helper
                .getReadableDatabase()
                .rawQuery(
                    "SELECT user_id,step,display_name,grade,subjects,goal,minutes,consented_at FROM"
                        + " learner_profiles WHERE user_id=?",
                    new String[] {Integer.toString(id)})) {
      return c.moveToFirst()
          ? new LearnerProfile(
              c.getInt(0),
              c.getInt(1),
              c.getString(2),
              c.getInt(3),
              c.getString(4),
              c.getInt(5),
              c.getInt(6),
              c.getLong(7))
          : null;
    }
  }

  public void advance(int id, int step) {
    if (step < 0 || step > OnboardingPolicy.PROFILE)
      throw new IllegalArgumentException("Invalid onboarding step");
    try (DatabaseHelper h = new DatabaseHelper(context)) {
      h.getWritableDatabase()
          .execSQL(
              "UPDATE learner_profiles SET step=MAX(step,?) WHERE user_id=? AND step<5",
              new Object[] {step, id});
    }
  }

  private ContentValues values(LearnerProfile p) {
    ContentValues v = new ContentValues();
    v.put("display_name", p.displayName.trim());
    v.put("grade", p.grade);
    v.put("subjects", p.subjects);
    v.put("goal", p.goal);
    v.put("minutes", p.minutes);
    return v;
  }

  public void saveDraft(LearnerProfile p) {
    try (DatabaseHelper h = new DatabaseHelper(context)) {
      h.getWritableDatabase()
          .update(
              "learner_profiles",
              values(p),
              "user_id=? AND step<5",
              new String[] {Integer.toString(p.userId)});
    }
  }

  public void complete(LearnerProfile p) {
    if (!OnboardingPolicy.valid(p)) throw new IllegalArgumentException("Invalid learner profile");
    try (DatabaseHelper h = new DatabaseHelper(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      db.beginTransaction();
      try {
        ContentValues v = values(p);
        v.put("step", OnboardingPolicy.COMPLETE);
        if (db.update("learner_profiles", v, "user_id=?", new String[] {Integer.toString(p.userId)})
            != 1) throw new SQLiteException("Missing learner profile");
        ContentValues user = new ContentValues();
        user.put(DatabaseHelper.COL_FULLNAME, p.displayName.trim());
        if (db.update("users", user, "id=?", new String[] {Integer.toString(p.userId)}) != 1)
          throw new SQLiteException("Missing account");
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
    }
  }
}
