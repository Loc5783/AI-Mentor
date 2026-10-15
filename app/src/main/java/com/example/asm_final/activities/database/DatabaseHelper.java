package com.example.asm_final.activities.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "user_db";
    private static final int DB_VERSION = 2;
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

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_USERS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USERNAME + " TEXT UNIQUE, "
                + COL_PASSWORD + " TEXT, "
                + COL_FULLNAME + " TEXT, "
                + COL_ADDRESS + " TEXT, "
                + COL_PHONE + " TEXT, "
                + COL_EMAIL + " TEXT, "
                + COL_AVATAR + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_FULLNAME + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_ADDRESS + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_PHONE + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_EMAIL + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_AVATAR + " TEXT");
        }
    }

    public long addUser(String username, String password, String email, String phone) {
        if (isBlank(username) || isBlank(password)) return -1;

        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username.trim());
        values.put(COL_PASSWORD, hashPassword(password));
        values.put(COL_EMAIL, safe(email));
        values.put(COL_PHONE, safe(phone));
        return getWritableDatabase().insert(TABLE_USERS, null, values);
    }

    public boolean checkLogin(String username, String password) {
        if (isBlank(username) || password == null) return false;

        SQLiteDatabase db = getWritableDatabase();
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_PASSWORD},
                COL_USERNAME + "=?",
                new String[]{username.trim()},
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
                    return BCrypt.verifyer()
                            .verify(password.toCharArray(), storedPassword)
                            .verified;
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
                db.update(
                        TABLE_USERS,
                        migrated,
                        COL_USERNAME + "=?",
                        new String[]{username.trim()}
                );
                return true;
            }
            return false;
        }
    }

    public int getUserId(String username) {
        if (isBlank(username)) return 0;

        try (Cursor cursor = getReadableDatabase().query(
                TABLE_USERS,
                new String[]{COL_ID},
                COL_USERNAME + "=?",
                new String[]{username.trim()},
                null,
                null,
                null,
                "1")) {
            if (!cursor.moveToFirst()) return 0;
            int idIndex = cursor.getColumnIndex(COL_ID);
            return idIndex < 0 ? 0 : Math.max(0, cursor.getInt(idIndex));
        }
    }

    public Cursor getUserData(String username) {
        return getReadableDatabase().query(
                TABLE_USERS,
                null,
                COL_USERNAME + "=?",
                new String[]{safe(username)},
                null,
                null,
                null,
                "1"
        );
    }

    public boolean updateUser(String username, String fullName, String address,
                              String phone, String email, String password, String avatar) {
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

        return getWritableDatabase().update(
                TABLE_USERS,
                values,
                COL_USERNAME + "=?",
                new String[]{username.trim()}
        ) > 0;
    }

    /** Debug helper retained for compatibility, but it no longer exposes password hashes. */
    public String getAllUsersString() {
        StringBuilder output = new StringBuilder();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_USERS,
                new String[]{COL_USERNAME},
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

    private static String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
    }

    private static boolean looksLikeBcryptHash(String value) {
        return value != null && value.startsWith("$2");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
