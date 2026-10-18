package com.example.asm_final.activities.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.mentor.data.ModuleDatabaseHelper;

/*
 * Role: Local data-layer class for App Shell, responsible for SQLite and data read/write operations.
 * Purpose: Separating the database from the UI centralizes queries, improves maintainability, and reduces direct SQL dependencies in Activities and Fragments.
 */
public class DatabaseHelper extends ModuleDatabaseHelper {

  private static final String DB_NAME = "user_db";
  private static final int DB_VERSION = 3;
  private static final int BCRYPT_COST = 12;

  private static final String TABLE_USERS = "users";
  private static final String COL_ID = "id";
  private static final String COL_USERNAME = "username";
  private static final String COL_PASSWORD = "password";

  public static final String COL_FULLNAME = "full_name";
  public static final String COL_ADDRESS = "address";
  public static final String COL_PHONE = "phone";
  public static final String COL_EMAIL = "email";
  public static final String COL_AVATAR = "avatar";

  /*
   * Responsibility: Initializes the `DatabaseHelper` object and sets up the required dependencies and initial data.
   * Purpose: The constructor ensures the object starts in a valid state immediately after creation.
   */
  public DatabaseHelper(Context context) {
    super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String createTable =
        "CREATE TABLE "
            + TABLE_USERS
            + "("
            + COL_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_USERNAME
            + " TEXT UNIQUE, "
            + COL_PASSWORD
            + " TEXT, "
            + COL_FULLNAME
            + " TEXT, "
            + COL_ADDRESS
            + " TEXT, "
            + COL_PHONE
            + " TEXT, "
            + COL_EMAIL
            + " TEXT, "
            + COL_AVATAR
            + " TEXT)";
    db.execSQL(createTable);
    createLearnerProfiles(db);
  }

  /*
   * Responsibility: Handles the Android callback or event `onUpgrade` and updates the related state.
   * Purpose: Keeping callbacks separate makes event flow clearer and follows the Android lifecycle cleanly.
   */
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
      db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_FULLNAME + " TEXT");
      db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_ADDRESS + " TEXT");
      db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_PHONE + " TEXT");
      db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_EMAIL + " TEXT");
      db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_AVATAR + " TEXT");
    }
    if (oldVersion < 3) {
      createLearnerProfiles(db);
      db.execSQL(
          "INSERT OR IGNORE INTO learner_profiles(user_id,step,display_name) SELECT"
              + " id,5,COALESCE(NULLIF(full_name,''),username,'') FROM users");
    }
  }

  private void createLearnerProfiles(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS learner_profiles(user_id INTEGER PRIMARY KEY REFERENCES"
            + " users(id) ON DELETE CASCADE,step INTEGER NOT NULL DEFAULT 0,display_name TEXT NOT"
            + " NULL DEFAULT '',grade INTEGER NOT NULL DEFAULT 0,subjects TEXT NOT NULL DEFAULT"
            + " '',goal INTEGER NOT NULL DEFAULT -1,minutes INTEGER NOT NULL DEFAULT 0,consented_at"
            + " INTEGER NOT NULL DEFAULT 0)");
  }

  /** Compatibility creation for imported/classic accounts and developer demos. */
  private long insertAccount(
      SQLiteDatabase db,
      String username,
      String password,
      String email,
      String phone,
      int step,
      String name,
      long consentedAt) {
    ContentValues values = new ContentValues();
    values.put(COL_USERNAME, username.trim());
    values.put(COL_PASSWORD, hashPassword(password));
    values.put(COL_EMAIL, safe(email));
    values.put(COL_PHONE, safe(phone));
    values.put(COL_FULLNAME, name);
    long id = db.insert(TABLE_USERS, null, values);
    if (id > 0) {
      ContentValues profile = new ContentValues();
      profile.put("user_id", id);
      profile.put("step", step);
      profile.put("display_name", name);
      profile.put("consented_at", consentedAt);
      if (db.insertOrThrow("learner_profiles", null, profile) < 0)
        throw new android.database.sqlite.SQLiteException("Profile insert failed");
    }
    return id;
  }

  public long addLearner(String name, String email, String password) {
    if (!com.example.asm_final.onboarding.OnboardingPolicy.validName(name)
        || email == null
        || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
        || !com.example.asm_final.accounts.PasswordPolicy.isValid(password)) return -1;
    String normalized = email.trim().toLowerCase(java.util.Locale.ROOT);
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      try (Cursor existing =
          db.rawQuery(
              "SELECT 1 FROM users WHERE email=? COLLATE NOCASE OR username=? COLLATE NOCASE LIMIT"
                  + " 1",
              new String[] {normalized, normalized})) {
        if (existing.moveToFirst()) return -1;
      }
      long id =
          insertAccount(
              db, normalized, password, normalized, "", 0, name.trim(), System.currentTimeMillis());
      db.setTransactionSuccessful();
      return id;
    } finally {
      db.endTransaction();
    }
  }

  /** An ambiguous legacy email never selects an arbitrary account. */
  public String canonicalUsername(String identifier) {
    if (isBlank(identifier)) return "";
    SQLiteDatabase db = getReadableDatabase();
    try (Cursor exact =
        db.rawQuery(
            "SELECT username FROM users WHERE username=? LIMIT 1",
            new String[] {identifier.trim()})) {
      if (exact.moveToFirst()) return exact.getString(0);
    }
    if (identifier.indexOf('@') < 0) return "";
    try (Cursor emails =
        db.rawQuery(
            "SELECT username FROM users WHERE email=? COLLATE NOCASE OR username=? COLLATE NOCASE"
                + " LIMIT 2",
            new String[] {identifier.trim(), identifier.trim()})) {
      if (!emails.moveToFirst()) return "";
      String name = emails.getString(0);
      return emails.moveToNext() ? "" : name;
    }
  }

  /*
   * Responsibility: Saves or adds data for `add user` to the appropriate storage or collection.
   * Purpose: Centralizing write operations helps maintain data consistency and handle storage errors.
   */
  public long addUser(String username, String password, String email, String phone) {
    if (isBlank(username) || isBlank(password)) return -1;

    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      long id = insertAccount(db, username, password, email, phone, 5, username.trim(), 0);
      db.setTransactionSuccessful();
      return id;
    } finally {
      db.endTransaction();
    }
  }

  /*
   * Responsibility: Validates the conditions or rules handled by `check login` and returns the result to the caller.
   * Purpose: A dedicated validation method keeps rules consistent and easier to test.
   */
  public boolean checkLogin(String username, String password) {
    if (isBlank(username) || password == null) return false;

    SQLiteDatabase db = getWritableDatabase();
    try (Cursor cursor =
        db.query(
            TABLE_USERS,
            new String[] {COL_PASSWORD},
            COL_USERNAME + "=?",
            new String[] {username.trim()},
            null,
            null,
            null,
            "1")) {
      if (!cursor.moveToFirst()) return false;

      int passIndex = cursor.getColumnIndex(COL_PASSWORD);
      if (passIndex < 0) return false;

      String storedPassword = cursor.getString(passIndex);
      if (isBlank(storedPassword)) return false;

      if (looksLikeBcryptHash(storedPassword)) {
        try {
          return BCrypt.verifyer().verify(password.toCharArray(), storedPassword).verified;
        } catch (RuntimeException invalidHash) {
          return false;
        }
      }

      // Repairs accounts affected by the old profile-update bug, which stored
      // the edited password as plaintext. A successful legacy login is
      // immediately migrated back to BCrypt.
      if (password.equals(storedPassword)) {
        ContentValues migrated = new ContentValues();
        migrated.put(COL_PASSWORD, hashPassword(password));
        db.update(TABLE_USERS, migrated, COL_USERNAME + "=?", new String[] {username.trim()});
        return true;
      }
      return false;
    }
  }

  /*
   * Responsibility: Reads or loads data for `get user id` from the appropriate data source.
   * Purpose: Centralizing read operations makes null handling, error handling, and data sources clearer.
   */
  public int getUserId(String username) {
    if (isBlank(username)) return 0;

    try (Cursor cursor =
        getReadableDatabase()
            .query(
                TABLE_USERS,
                new String[] {COL_ID},
                COL_USERNAME + "=?",
                new String[] {username.trim()},
                null,
                null,
                null,
                "1")) {
      if (!cursor.moveToFirst()) return 0;
      int idIndex = cursor.getColumnIndex(COL_ID);
      return idIndex < 0 ? 0 : Math.max(0, cursor.getInt(idIndex));
    }
  }

  /*
   * Responsibility: Reads or loads data for `get user data` from the appropriate data source.
   * Purpose: Centralizing read operations makes null handling, error handling, and data sources clearer.
   */
  public Cursor getUserData(String username) {
    return getReadableDatabase()
        .query(
            TABLE_USERS,
            null,
            COL_USERNAME + "=?",
            new String[] {safe(username)},
            null,
            null,
            null,
            "1");
  }

  /*
   * Responsibility: Updates or synchronizes `update user` with the latest data or state.
   * Purpose: Keeps the UI and data synchronized after user actions.
   */
  public boolean updateUser(
      String username,
      String fullName,
      String address,
      String phone,
      String email,
      String password,
      String avatar) {
    if (isBlank(username)) return false;

    ContentValues values = new ContentValues();
    values.put(COL_FULLNAME, safe(fullName));
    values.put(COL_ADDRESS, safe(address));
    values.put(COL_PHONE, safe(phone));
    values.put(COL_EMAIL, safe(email));
    values.put(COL_AVATAR, safe(avatar));

    // Keep the current password when the field is empty. When a new password
    // is supplied, store it using the same BCrypt policy as registration.
    if (!isBlank(password)) {
      values.put(COL_PASSWORD, hashPassword(password));
    }

    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      boolean updated =
          db.update(TABLE_USERS, values, COL_USERNAME + "=?", new String[] {username.trim()}) > 0;
      if (updated) {
        ContentValues display = new ContentValues();
        display.put("display_name", isBlank(fullName) ? username.trim() : fullName.trim());
        db.update(
            "learner_profiles",
            display,
            "user_id=?",
            new String[] {Integer.toString(getUserId(username))});
      }
      db.setTransactionSuccessful();
      return updated;
    } finally {
      db.endTransaction();
    }
  }

  /** Debug helper retained for compatibility, but it no longer exposes password hashes. */
  /*
   * Responsibility: Reads or loads data for `get all users string` from the appropriate data source.
   * Purpose: Centralizing read operations makes null handling, error handling, and data sources clearer.
   */
  public String getAllUsersString() {
    StringBuilder output = new StringBuilder();
    try (Cursor cursor =
        getReadableDatabase()
            .query(
                TABLE_USERS,
                new String[] {COL_USERNAME},
                null,
                null,
                null,
                null,
                COL_USERNAME + " COLLATE NOCASE ASC")) {
      int userIndex = cursor.getColumnIndex(COL_USERNAME);
      while (cursor.moveToNext()) {
        if (userIndex >= 0) {
          output.append("👤 ").append(cursor.getString(userIndex)).append('\n');
        }
      }
    }
    return output.toString();
  }

  /*
   * Responsibility: Validates the conditions or rules handled by `hash password` and returns the result to the caller.
   * Purpose: A dedicated validation method keeps rules consistent and easier to test.
   */
  private static String hashPassword(String password) {
    return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
  }

  private static boolean looksLikeBcryptHash(String value) {
    return value != null && value.startsWith("$2");
  }

  /*
   * Responsibility: Validates the conditions or rules handled by `is blank` and returns the result to the caller.
   * Purpose: A dedicated validation method keeps rules consistent and easier to test.
   */
  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static String safe(String value) {
    return value == null ? "" : value.trim();
  }
}
