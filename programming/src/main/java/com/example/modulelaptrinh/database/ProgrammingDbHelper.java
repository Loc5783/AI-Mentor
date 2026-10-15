package com.example.modulelaptrinh.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.modulelaptrinh.ProgrammingMessage;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ProgrammingDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "ProgrammingDbHelper";
    private static final String DATABASE_NAME = "ProgrammingMentor.db";
    private static final int DATABASE_VERSION = 6;

    // Bảng sessions
    public static final String TABLE_SESSIONS = "sessions";
    public static final String COLUMN_SESSION_ID = "id";
    public static final String COLUMN_SESSION_NAME = "name";
    public static final String COLUMN_SESSION_USER_ID = "user_id";
    public static final String COLUMN_SESSION_TIMESTAMP = "timestamp";

    // Bảng messages
    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_MESSAGE_ID = "id";
    public static final String COLUMN_MESSAGE_SESSION_ID = "session_id";
    public static final String COLUMN_MESSAGE_CONTENT = "content";
    public static final String COLUMN_MESSAGE_IS_USER = "is_user";
    public static final String COLUMN_MESSAGE_IMAGES = "images";
    public static final String COLUMN_MESSAGE_TIMESTAMP = "timestamp";

    // Bảng chat history cũ
    public static final String TABLE_CHAT_HISTORY = "chat_history";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_QUESTION = "question";
    public static final String COLUMN_ANSWER = "answer";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    // Bảng notes
    public static final String TABLE_NOTES = "notes";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_NOTE_USER_ID = "user_id";

    // ===== QUIZ TABLES =====
    public static final String TABLE_QUIZ_ATTEMPTS = "programming_quiz_attempts";
    public static final String COLUMN_QUIZ_ID = "id";
    public static final String COLUMN_QUIZ_USER_ID = "user_id";
    public static final String COLUMN_QUIZ_SUBJECT = "subject";
    public static final String COLUMN_QUIZ_LEVEL = "level";
    public static final String COLUMN_QUIZ_TOPIC = "topic";
    public static final String COLUMN_QUIZ_TOTAL = "total_questions";
    public static final String COLUMN_QUIZ_CORRECT = "correct_answers";
    public static final String COLUMN_QUIZ_EXP_EARNED = "exp_earned";
    public static final String COLUMN_QUIZ_TIMESTAMP = "created_at";

    public static final String TABLE_USER_PROGRESS = "user_progress";
    public static final String COLUMN_PROGRESS_USER_ID = "user_id";
    public static final String COLUMN_PROGRESS_TOTAL_EXP = "total_exp";
    public static final String COLUMN_PROGRESS_SUBJECT = "subject";
    public static final String COLUMN_PROGRESS_UPDATED_AT = "updated_at";

    // ===== CREATE TABLE STATEMENTS =====
    private static final String CREATE_SESSIONS_TABLE =
            "CREATE TABLE " + TABLE_SESSIONS + " (" +
                    COLUMN_SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_SESSION_USER_ID + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_SESSION_NAME + " TEXT, " +
                    COLUMN_SESSION_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    private static final String CREATE_MESSAGES_TABLE =
            "CREATE TABLE " + TABLE_MESSAGES + " (" +
                    COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MESSAGE_SESSION_ID + " INTEGER, " +
                    COLUMN_MESSAGE_CONTENT + " TEXT, " +
                    COLUMN_MESSAGE_IS_USER + " INTEGER, " +
                    COLUMN_MESSAGE_IMAGES + " TEXT, " +
                    COLUMN_MESSAGE_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    private static final String CREATE_CHAT_TABLE =
            "CREATE TABLE " + TABLE_CHAT_HISTORY + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_QUESTION + " TEXT, " +
                    COLUMN_ANSWER + " TEXT, " +
                    COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    private static final String CREATE_NOTES_TABLE =
            "CREATE TABLE " + TABLE_NOTES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NOTE_USER_ID + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_CONTENT + " TEXT, " +
                    COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    private static final String CREATE_QUIZ_ATTEMPTS_TABLE =
            "CREATE TABLE " + TABLE_QUIZ_ATTEMPTS + " (" +
                    COLUMN_QUIZ_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_QUIZ_USER_ID + " INTEGER NOT NULL, " +
                    COLUMN_QUIZ_SUBJECT + " TEXT NOT NULL, " +
                    COLUMN_QUIZ_LEVEL + " TEXT NOT NULL, " +
                    COLUMN_QUIZ_TOPIC + " TEXT, " +
                    COLUMN_QUIZ_TOTAL + " INTEGER NOT NULL, " +
                    COLUMN_QUIZ_CORRECT + " INTEGER NOT NULL, " +
                    COLUMN_QUIZ_EXP_EARNED + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_QUIZ_TIMESTAMP + " INTEGER NOT NULL" +
                    ");";

    private static final String CREATE_USER_PROGRESS_TABLE =
            "CREATE TABLE " + TABLE_USER_PROGRESS + " (" +
                    COLUMN_PROGRESS_USER_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_PROGRESS_TOTAL_EXP + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_PROGRESS_SUBJECT + " TEXT NOT NULL, " +
                    COLUMN_PROGRESS_UPDATED_AT + " INTEGER NOT NULL" +
                    ");";

    public ProgrammingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SESSIONS_TABLE);
        db.execSQL(CREATE_MESSAGES_TABLE);
        db.execSQL(CREATE_CHAT_TABLE);
        db.execSQL(CREATE_NOTES_TABLE);
        db.execSQL(CREATE_QUIZ_ATTEMPTS_TABLE);
        db.execSQL(CREATE_USER_PROGRESS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 2) {
            db.execSQL(CREATE_SESSIONS_TABLE);
            db.execSQL(CREATE_MESSAGES_TABLE);
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_MESSAGES + " ADD COLUMN " + COLUMN_MESSAGE_IMAGES + " TEXT");
            } catch (Exception e) {
                Log.w(TAG, "Column already exists or migration error: " + e.getMessage());
            }
        }
        if (oldVersion < 4) {
            // Nâng cấp lên version 4
        }
        if (oldVersion < 5) {
            try {
                db.execSQL(CREATE_QUIZ_ATTEMPTS_TABLE);
                db.execSQL(CREATE_USER_PROGRESS_TABLE);
                Log.d(TAG, "Quiz tables created successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error creating quiz tables: " + e.getMessage());
            }
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_SESSIONS
                        + " ADD COLUMN " + COLUMN_SESSION_USER_ID
                        + " INTEGER NOT NULL DEFAULT 0");
            } catch (Exception e) {
                Log.w(TAG, "Session user migration skipped: " + e.getMessage());
            }
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES
                        + " ADD COLUMN " + COLUMN_NOTE_USER_ID
                        + " INTEGER NOT NULL DEFAULT 0");
            } catch (Exception e) {
                Log.w(TAG, "Note user migration skipped: " + e.getMessage());
            }
        }
    }

    // ===== SESSION METHODS =====
    public long createNewSession(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SESSION_USER_ID, Math.max(0, userId));
        values.put(COLUMN_SESSION_NAME, "Cuộc trò chuyện mới");
        long id = db.insert(TABLE_SESSIONS, null, values);
        db.close();
        return id;
    }

    public void updateSessionName(long sessionId, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SESSION_NAME, name);
        db.update(TABLE_SESSIONS, values, COLUMN_SESSION_ID + " = ?", new String[]{String.valueOf(sessionId)});
        db.close();
    }

    public boolean isSessionOwnedByUser(long sessionId, int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_SESSIONS,
                new String[]{COLUMN_SESSION_ID},
                COLUMN_SESSION_ID + "=? AND " + COLUMN_SESSION_USER_ID + "=?",
                new String[]{String.valueOf(sessionId), String.valueOf(Math.max(0, userId))},
                null, null, null
        );
        boolean owned = cursor.moveToFirst();
        cursor.close();
        db.close();
        return owned;
    }

    public List<SessionItem> getAllSessions(int userId) {
        List<SessionItem> sessionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SESSIONS
                + " WHERE " + COLUMN_SESSION_USER_ID + "=?"
                + " ORDER BY " + COLUMN_SESSION_TIMESTAMP + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(Math.max(0, userId))});

        if (cursor.moveToFirst()) {
            do {
                SessionItem session = new SessionItem();
                session.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID)));
                session.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_NAME)));
                session.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_TIMESTAMP)));
                sessionList.add(session);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return sessionList;
    }

    public void deleteSession(long sessionId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deleted = db.delete(TABLE_SESSIONS,
                COLUMN_SESSION_ID + "=? AND " + COLUMN_SESSION_USER_ID + "=?",
                new String[]{String.valueOf(sessionId), String.valueOf(Math.max(0, userId))});
        if (deleted > 0) {
            db.delete(TABLE_MESSAGES, COLUMN_MESSAGE_SESSION_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});
        }
        db.close();
    }

    public void deleteAllSessions(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int safeUserId = Math.max(0, userId);
        db.execSQL("DELETE FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_MESSAGE_SESSION_ID
                + " IN (SELECT " + COLUMN_SESSION_ID + " FROM " + TABLE_SESSIONS
                + " WHERE " + COLUMN_SESSION_USER_ID + "=?)", new Object[]{safeUserId});
        db.delete(TABLE_SESSIONS, COLUMN_SESSION_USER_ID + "=?",
                new String[]{String.valueOf(safeUserId)});
        db.close();
    }

    // ===== MESSAGE METHODS =====
    public long insertMessage(long sessionId, String content, boolean isUser) {
        return insertMessage(sessionId, content, isUser, null);
    }

    public long insertMessage(long sessionId, String content, boolean isUser, List<String> imageBase64List) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE_SESSION_ID, sessionId);
        values.put(COLUMN_MESSAGE_CONTENT, content);
        values.put(COLUMN_MESSAGE_IS_USER, isUser ? 1 : 0);

        String imagesJson = null;
        if (imageBase64List != null && !imageBase64List.isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            for (String image : imageBase64List) {
                jsonArray.put(image);
            }
            imagesJson = jsonArray.toString();
        }
        values.put(COLUMN_MESSAGE_IMAGES, imagesJson);

        long id = db.insert(TABLE_MESSAGES, null, values);
        db.close();
        return id;
    }

    public List<ProgrammingMessage> getMessagesBySession(long sessionId) {
        List<ProgrammingMessage> messageList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_MESSAGES +
                " WHERE " + COLUMN_MESSAGE_SESSION_ID + " = ?" +
                " ORDER BY " + COLUMN_MESSAGE_TIMESTAMP + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(sessionId)});

        if (cursor.moveToFirst()) {
            do {
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_CONTENT));
                boolean isUser = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_IS_USER)) == 1;
                String imagesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_IMAGES));

                List<String> imageBase64List = new ArrayList<>();
                if (imagesJson != null && !imagesJson.isEmpty()) {
                    try {
                        JSONArray jsonArray = new JSONArray(imagesJson);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            imageBase64List.add(jsonArray.getString(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                ProgrammingMessage message = new ProgrammingMessage(content, isUser, imageBase64List);
                messageList.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return messageList;
    }

    // ===== CHAT HISTORY METHODS =====
    public long insertChatHistory(String question, String answer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUESTION, question);
        values.put(COLUMN_ANSWER, answer);
        long id = db.insert(TABLE_CHAT_HISTORY, null, values);
        db.close();
        return id;
    }

    public List<ProgrammingChatHistoryItem> getAllChatHistory() {
        List<ProgrammingChatHistoryItem> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_CHAT_HISTORY + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                ProgrammingChatHistoryItem item = new ProgrammingChatHistoryItem();
                item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                item.setQuestion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUESTION)));
                item.setAnswer(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANSWER)));
                item.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                historyList.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return historyList;
    }

    public void deleteChatHistory(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHAT_HISTORY, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteAllChatHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHAT_HISTORY, null, null);
        db.close();
    }

    // ===== NOTES METHODS =====
    public long insertNote(int userId, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_USER_ID, Math.max(0, userId));
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    public List<ProgrammingNote> getAllNotes(int userId) {
        List<ProgrammingNote> noteList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NOTES
                + " WHERE " + COLUMN_NOTE_USER_ID + "=?"
                + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(Math.max(0, userId))});

        if (cursor.moveToFirst()) {
            do {
                ProgrammingNote note = new ProgrammingNote();
                note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)));
                note.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                noteList.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return noteList;
    }

    public List<ProgrammingNote> searchNotes(int userId, String keyword) {
        List<ProgrammingNote> noteList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NOTES
                + " WHERE " + COLUMN_NOTE_USER_ID + "=? AND ("
                + COLUMN_TITLE + " LIKE ? OR " + COLUMN_CONTENT + " LIKE ?)"
                + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        String[] args = {String.valueOf(Math.max(0, userId)),
                "%" + keyword + "%", "%" + keyword + "%"};
        Cursor cursor = db.rawQuery(query, args);

        if (cursor.moveToFirst()) {
            do {
                ProgrammingNote note = new ProgrammingNote();
                note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)));
                note.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                noteList.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return noteList;
    }

    public void deleteNote(int userId, int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES,
                COLUMN_ID + "=? AND " + COLUMN_NOTE_USER_ID + "=?",
                new String[]{String.valueOf(id), String.valueOf(Math.max(0, userId))});
        db.close();
    }

    public void updateNote(int userId, int id, String title, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        db.update(TABLE_NOTES, values,
                COLUMN_ID + "=? AND " + COLUMN_NOTE_USER_ID + "=?",
                new String[]{String.valueOf(id), String.valueOf(Math.max(0, userId))});
        db.close();
    }

    // ===== QUIZ METHODS =====

    public long saveQuizResult(int userId, String subject, String level, String topic,
                               int totalQuestions, int correctAnswers, int expEarned) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUIZ_USER_ID, userId);
        values.put(COLUMN_QUIZ_SUBJECT, subject != null ? subject : "Lập trình");
        values.put(COLUMN_QUIZ_LEVEL, level != null ? level : "Cơ bản");
        values.put(COLUMN_QUIZ_TOPIC, topic != null ? topic : "Kiến thức lập trình");
        values.put(COLUMN_QUIZ_TOTAL, Math.max(0, totalQuestions));
        values.put(COLUMN_QUIZ_CORRECT, Math.max(0, correctAnswers));
        values.put(COLUMN_QUIZ_EXP_EARNED, Math.max(0, expEarned));
        values.put(COLUMN_QUIZ_TIMESTAMP, System.currentTimeMillis());

        long id = db.insert(TABLE_QUIZ_ATTEMPTS, null, values);
        db.close();

        if (id != -1) {
            updateUserExp(userId, subject, expEarned);
        }

        return id;
    }

    /**
     * Cập nhật tổng EXP của user - SỬA LỖI UNIQUE CONSTRAINT
     */
    private void updateUserExp(int userId, String subject, int expEarned) {
        SQLiteDatabase db = this.getWritableDatabase();
        String safeSubject = subject != null ? subject : "Lập trình";

        if (expEarned <= 0) {
            db.close();
            return;
        }

        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_USER_PROGRESS,
                    new String[]{COLUMN_PROGRESS_TOTAL_EXP},
                    COLUMN_PROGRESS_USER_ID + "=? AND " + COLUMN_PROGRESS_SUBJECT + "=?",
                    new String[]{String.valueOf(userId), safeSubject},
                    null, null, null
            );

            ContentValues values = new ContentValues();
            values.put(COLUMN_PROGRESS_UPDATED_AT, System.currentTimeMillis());

            if (cursor != null && cursor.moveToFirst()) {
                int currentExp = cursor.getInt(0);
                values.put(COLUMN_PROGRESS_TOTAL_EXP, currentExp + expEarned);
                // UPDATE thay vì INSERT
                db.update(TABLE_USER_PROGRESS, values,
                        COLUMN_PROGRESS_USER_ID + "=? AND " + COLUMN_PROGRESS_SUBJECT + "=?",
                        new String[]{String.valueOf(userId), safeSubject});
                Log.d(TAG, "Updated EXP for user " + userId + ": " + currentExp + " + " + expEarned + " = " + (currentExp + expEarned));
            } else {
                values.put(COLUMN_PROGRESS_USER_ID, userId);
                values.put(COLUMN_PROGRESS_SUBJECT, safeSubject);
                values.put(COLUMN_PROGRESS_TOTAL_EXP, expEarned);
                // Dùng INSERT OR REPLACE để tránh lỗi UNIQUE
                db.insertWithOnConflict(TABLE_USER_PROGRESS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                Log.d(TAG, "Inserted new EXP for user " + userId + ": " + expEarned);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi update EXP: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    public int getTotalExp(int userId, String subject) {
        SQLiteDatabase db = this.getReadableDatabase();
        String safeSubject = subject != null ? subject : "Lập trình";

        Cursor cursor = db.query(
                TABLE_USER_PROGRESS,
                new String[]{COLUMN_PROGRESS_TOTAL_EXP},
                COLUMN_PROGRESS_USER_ID + "=? AND " + COLUMN_PROGRESS_SUBJECT + "=?",
                new String[]{String.valueOf(userId), safeSubject},
                null, null, null
        );
        int exp = 0;
        if (cursor.moveToFirst()) {
            exp = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return exp;
    }

    public double getAverageAccuracy(int userId, String subject) {
        SQLiteDatabase db = this.getReadableDatabase();
        String safeSubject = subject != null ? subject : "Lập trình";

        String sql = "SELECT COALESCE(SUM(" + COLUMN_QUIZ_CORRECT + "),0), "
                + "COALESCE(SUM(" + COLUMN_QUIZ_TOTAL + "),0) FROM "
                + TABLE_QUIZ_ATTEMPTS + " WHERE "
                + COLUMN_QUIZ_USER_ID + "=? AND "
                + COLUMN_QUIZ_SUBJECT + "=?";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), safeSubject});
        double accuracy = 0;
        if (cursor.moveToFirst()) {
            int correct = cursor.getInt(0);
            int total = cursor.getInt(1);
            accuracy = total == 0 ? 0 : (correct * 100.0) / total;
        }
        cursor.close();
        db.close();
        return accuracy;
    }

    public int getQuizAttemptCount(int userId, String subject) {
        SQLiteDatabase db = this.getReadableDatabase();
        String safeSubject = subject != null ? subject : "Lập trình";

        Cursor cursor = db.query(
                TABLE_QUIZ_ATTEMPTS,
                new String[]{"COUNT(*)"},
                COLUMN_QUIZ_USER_ID + "=? AND " + COLUMN_QUIZ_SUBJECT + "=?",
                new String[]{String.valueOf(userId), safeSubject},
                null, null, null
        );
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getTotalExpAllSubjects(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT COALESCE(SUM(" + COLUMN_PROGRESS_TOTAL_EXP + "),0) FROM "
                + TABLE_USER_PROGRESS + " WHERE "
                + COLUMN_PROGRESS_USER_ID + "=?";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        int total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    public Cursor getUserProgress(int userId, String subject) {
        SQLiteDatabase db = this.getReadableDatabase();
        String safeSubject = subject != null ? subject : "Lập trình";

        return db.query(
                TABLE_USER_PROGRESS,
                null,
                COLUMN_PROGRESS_USER_ID + "=? AND " + COLUMN_PROGRESS_SUBJECT + "=?",
                new String[]{String.valueOf(userId), safeSubject},
                null, null, null
        );
    }
}