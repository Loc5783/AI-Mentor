package com.example.asm_final.learning;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.mentor.data.ModuleDatabaseHelper;
import com.example.mentor.learning.SubjectId;
import java.util.*;

/**
 * Enrollment choices survive process restarts and are isolated from legacy onboarding interests.
 */
public final class LearningSetupStore extends ModuleDatabaseHelper {
  public LearningSetupStore(Context context) {
    super(context, "learning_setup", null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE mentor_enrollments(id TEXT PRIMARY KEY,user_id INTEGER NOT NULL REFERENCES"
            + " users(id) ON DELETE CASCADE,subject_id TEXT NOT NULL,grade INTEGER NOT NULL"
            + " CHECK(grade BETWEEN 10 AND 12),UNIQUE(user_id,subject_id,grade))");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int old, int version) {
    throw new IllegalStateException("Missing enrollment migration");
  }

  public Set<SubjectId> selected(int user, int grade) {
    EnumSet<SubjectId> result = EnumSet.noneOf(SubjectId.class);
    try (Cursor c =
        getReadableDatabase()
            .rawQuery(
                "SELECT subject_id FROM mentor_enrollments WHERE user_id=? AND grade=?",
                new String[] {Integer.toString(user), Integer.toString(grade)})) {
      while (c.moveToNext()) result.add(SubjectId.fromWireId(c.getString(0)));
    }
    return result;
  }

  public void save(int user, int grade, Set<SubjectId> selected) {
    if (user <= 0
        || grade < 10
        || grade > 12
        || !EnumSet.of(SubjectId.BIOLOGY, SubjectId.CHEMISTRY, SubjectId.PHYSICS)
            .containsAll(selected)) throw new IllegalArgumentException("Unsupported lesson scope");
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      Set<SubjectId> before = selected(user, grade);
      for (SubjectId id : before)
        if (!selected.contains(id))
          db.delete(
              "mentor_enrollments",
              "user_id=? AND grade=? AND subject_id=?",
              new String[] {Integer.toString(user), Integer.toString(grade), id.wireId});
      for (SubjectId id : selected)
        if (!before.contains(id)) {
          ContentValues v = new ContentValues();
          v.put("id", UUID.randomUUID().toString());
          v.put("user_id", user);
          v.put("grade", grade);
          v.put("subject_id", id.wireId);
          db.insertOrThrow("mentor_enrollments", null, v);
        }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }
}
