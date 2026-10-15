package com.brightpath.science.data.local;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Database owned only by the Science module.
 * Keeping it separate prevents Science schema changes from affecting the existing app database.
 */
public final class ScienceDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "science_ai.db";
    private static final int DATABASE_VERSION = 5;

    public ScienceDbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ScienceDbContract.createHistoryTable());
        db.execSQL(ScienceDbContract.createBookmarkTable());
        db.execSQL(ScienceDbContract.createQuizAttemptTable());
        db.execSQL(ScienceDbContract.createNoteTable());
        createIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + ScienceDbContract.QuizAttempt.TABLE
                    + " ADD COLUMN " + ScienceDbContract.QuizAttempt.EDUCATION_LEVEL
                    + " TEXT NOT NULL DEFAULT 'MIDDLE_SCHOOL'");
            db.execSQL("ALTER TABLE " + ScienceDbContract.QuizAttempt.TABLE
                    + " ADD COLUMN " + ScienceDbContract.QuizAttempt.TOPIC + " TEXT");
            db.execSQL("ALTER TABLE " + ScienceDbContract.QuizAttempt.TABLE
                    + " ADD COLUMN " + ScienceDbContract.QuizAttempt.CHALLENGE
                    + " TEXT NOT NULL DEFAULT 'ON_LEVEL'");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + ScienceDbContract.QuizAttempt.TABLE
                    + " ADD COLUMN " + ScienceDbContract.QuizAttempt.EXP_EARNED
                    + " INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) {
            db.execSQL(ScienceDbContract.createNoteTable());
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + ScienceDbContract.History.TABLE
                    + " ADD COLUMN " + ScienceDbContract.History.CONVERSATION_ID
                    + " TEXT NOT NULL DEFAULT ''");
            db.execSQL("UPDATE " + ScienceDbContract.History.TABLE + " SET "
                    + ScienceDbContract.History.CONVERSATION_ID + " = 'legacy-' || "
                    + ScienceDbContract.History.ID + " WHERE "
                    + ScienceDbContract.History.CONVERSATION_ID + " = ''");
        }
        createIndexes(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop all tables and recreate them if downgraded.
        db.execSQL("DROP TABLE IF EXISTS " + ScienceDbContract.History.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ScienceDbContract.Bookmark.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ScienceDbContract.QuizAttempt.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ScienceDbContract.Note.TABLE);
        onCreate(db);
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_science_history_user_subject_time ON "
                + ScienceDbContract.History.TABLE + "("
                + ScienceDbContract.History.USER_ID + ","
                + ScienceDbContract.History.SUBJECT + ","
                + ScienceDbContract.History.CREATED_AT + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_science_history_conversation_time ON "
                + ScienceDbContract.History.TABLE + "("
                + ScienceDbContract.History.USER_ID + ","
                + ScienceDbContract.History.CONVERSATION_ID + ","
                + ScienceDbContract.History.CREATED_AT + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_science_notes_user_time ON "
                + ScienceDbContract.Note.TABLE + "("
                + ScienceDbContract.Note.USER_ID + ","
                + ScienceDbContract.Note.UPDATED_AT + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_science_quiz_adaptive ON "
                + ScienceDbContract.QuizAttempt.TABLE + "("
                + ScienceDbContract.QuizAttempt.USER_ID + ","
                + ScienceDbContract.QuizAttempt.SUBJECT + ","
                + ScienceDbContract.QuizAttempt.EDUCATION_LEVEL + ","
                + ScienceDbContract.QuizAttempt.CREATED_AT + ")");
    }

    public int getTotalExp(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT SUM(" + ScienceDbContract.QuizAttempt.EXP_EARNED + ") FROM "
                + ScienceDbContract.QuizAttempt.TABLE + " WHERE "
                + ScienceDbContract.QuizAttempt.USER_ID + "=?";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }
}
