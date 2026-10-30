package com.example.asm_final;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies profile-note migration keeps existing account rows and saves the new field. */
@RunWith(AndroidJUnit4.class)
public final class ProfileBioMigrationTest {
  private SQLiteDatabase database;

  @Before
  public void createVersionOneDatabase() {
    database = SQLiteDatabase.create(null);
    database.execSQL(
        "CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT,username TEXT UNIQUE,password"
            + " TEXT,full_name TEXT,address TEXT,phone TEXT,email TEXT,avatar TEXT)");
    database.execSQL(
        "INSERT INTO users(username,full_name,address,phone,email,avatar)"
            + " VALUES('learner@example.com','Lan','','','learner@example.com','')");
  }

  @After
  public void closeTestDatabase() {
    if (database != null) database.close();
  }

  @Test
  public void upgradePreservesAccountAndPersistsLearnerNote() {
    new com.example.asm_final.activities.database.DatabaseHelper(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .getTargetContext())
        .onUpgrade(database, 1, 2);
    try (android.database.Cursor migrated =
        database.query("users", new String[] {"full_name", "bio"}, null, null, null, null, null)) {
      assertTrue(migrated.moveToFirst());
      assertEquals("Lan", migrated.getString(0));
      assertEquals("", migrated.getString(1));
      database.execSQL("UPDATE users SET bio='Muốn học đều mỗi ngày' WHERE id=1");
    }
    try (android.database.Cursor saved =
        database.query("users", new String[] {"bio"}, "id=1", null, null, null, null)) {
      assertTrue(saved.moveToFirst());
      assertEquals("Muốn học đều mỗi ngày", saved.getString(0));
    }
  }
}
