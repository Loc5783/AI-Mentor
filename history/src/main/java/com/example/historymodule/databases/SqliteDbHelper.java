package com.example.historymodule.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.historymodule.HistoryMessage;

import java.util.ArrayList;
import java.util.List;

/** SQLite storage for History conversations, notes, Quiz attempts and EXP. */
public final class SqliteDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "history_db";
    private static final int DATABASE_VERSION = 8;

    public SqliteDbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createLegacyTables(db);
        createConversationTable(db);
        createQuizAttemptTable(db);
        ensureIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Non-destructive migration: preserve conversations, notes and Quiz history.
        createLegacyTables(db);
        createConversationTable(db);
        createQuizAttemptTable(db);
        ensureColumns(db);
        ensureIndexes(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            ensureColumns(db);
            ensureIndexes(db);
        }
    }

    private void createLegacyTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS history_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT NOT NULL DEFAULT '', "
                + "content TEXT NOT NULL DEFAULT '', "
                + "timestamp TEXT NOT NULL DEFAULT '', "
                + "type TEXT NOT NULL DEFAULT 'search')");

        db.execSQL("CREATE TABLE IF NOT EXISTS note_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT NOT NULL DEFAULT '', "
                + "content TEXT NOT NULL DEFAULT '', "
                + "category TEXT NOT NULL DEFAULT 'Khác', "
                + "user_id INTEGER NOT NULL DEFAULT 0, "
                + "pinned INTEGER NOT NULL DEFAULT 0, "
                + "created_at INTEGER NOT NULL DEFAULT 0, "
                + "updated_at INTEGER NOT NULL DEFAULT 0)");

        // Retained only so old installations can migrate without losing data.
        db.execSQL("CREATE TABLE IF NOT EXISTS quiz_sets ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, category TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS quiz_questions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, set_id INTEGER, question TEXT, "
                + "option_a TEXT, option_b TEXT, option_c TEXT, option_d TEXT, correct_option TEXT)");
    }

    private void createConversationTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chat_messages ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL DEFAULT 0, "
                + "conversation_id TEXT NOT NULL, "
                + "role TEXT NOT NULL, "
                + "content TEXT NOT NULL, "
                + "image_attached INTEGER NOT NULL DEFAULT 0, "
                + "created_at INTEGER NOT NULL)");
    }

    private void createQuizAttemptTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS quiz_attempts ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL DEFAULT 0, "
                + "topic TEXT NOT NULL, "
                + "education_level TEXT NOT NULL DEFAULT 'Trung học cơ sở', "
                + "correct_count INTEGER NOT NULL, "
                + "total_count INTEGER NOT NULL, "
                + "exp_earned INTEGER NOT NULL DEFAULT 0, "
                + "created_at INTEGER NOT NULL)");
    }

    private void ensureColumns(SQLiteDatabase db) {
        addColumnIfMissing(db, "note_items", "user_id", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, "note_items", "pinned", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, "note_items", "created_at", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, "note_items", "updated_at", "INTEGER NOT NULL DEFAULT 0");

        addColumnIfMissing(db, "chat_messages", "user_id", "INTEGER NOT NULL DEFAULT 0");

        addColumnIfMissing(db, "quiz_attempts", "user_id", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, "quiz_attempts", "education_level",
                "TEXT NOT NULL DEFAULT 'Trung học cơ sở'");
        addColumnIfMissing(db, "quiz_attempts", "exp_earned", "INTEGER NOT NULL DEFAULT 0");

        long now = System.currentTimeMillis();
        db.execSQL("UPDATE note_items SET created_at = ? WHERE created_at <= 0", new Object[]{now});
        db.execSQL("UPDATE note_items SET updated_at = created_at WHERE updated_at <= 0");
    }

    private void addColumnIfMissing(SQLiteDatabase db,
                                    String table,
                                    String column,
                                    String definition) {
        if (!columnExists(db, table, column)) {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean columnExists(SQLiteDatabase db, String table, String column) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (column.equalsIgnoreCase(cursor.getString(nameIndex))) return true;
            }
        }
        return false;
    }

    private void ensureIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_chat_user_conversation_time "
                + "ON chat_messages(user_id, conversation_id, created_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_notes_user_pin_time "
                + "ON note_items(user_id, pinned, updated_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_history_quiz_user_time "
                + "ON quiz_attempts(user_id, created_at)");
    }

    public long saveChatMessage(int userId,
                                String conversationId,
                                String role,
                                String content,
                                boolean imageAttached) {
        ContentValues values = new ContentValues();
        values.put("user_id", Math.max(0, userId));
        values.put("conversation_id", normalizeConversationId(conversationId));
        values.put("role", role == null ? HistoryMessage.SENT_BY_BOT : role);
        values.put("content", content == null ? "" : content.trim());
        values.put("image_attached", imageAttached ? 1 : 0);
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("chat_messages", null, values);
    }

    public List<HistoryMessage> loadChatMessages(int userId, String conversationId) {
        List<HistoryMessage> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "chat_messages",
                new String[]{"role", "content", "image_attached"},
                "user_id = ? AND conversation_id = ?",
                new String[]{String.valueOf(Math.max(0, userId)), normalizeConversationId(conversationId)},
                null,
                null,
                "created_at ASC, id ASC"
        )) {
            while (cursor.moveToNext()) {
                result.add(new HistoryMessage(
                        cursor.getString(1),
                        cursor.getString(0),
                        false,
                        cursor.getInt(2) == 1
                ));
            }
        }
        return result;
    }

    public List<HistoryHistoryItem> loadConversationSummaries(int userId,
                                                               long startInclusive,
                                                               long endExclusive,
                                                               int limit) {
        List<HistoryHistoryItem> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder()
                .append("SELECT c.conversation_id, MAX(c.created_at) AS last_time, ")
                .append("(SELECT first.content FROM chat_messages first ")
                .append(" WHERE first.user_id = c.user_id AND first.conversation_id = c.conversation_id ")
                .append(" AND first.role = ? ORDER BY first.created_at ASC, first.id ASC LIMIT 1) AS first_user, ")
                .append("(SELECT last.content FROM chat_messages last ")
                .append(" WHERE last.user_id = c.user_id AND last.conversation_id = c.conversation_id ")
                .append(" ORDER BY last.created_at DESC, last.id DESC LIMIT 1) AS last_content ")
                .append("FROM chat_messages c WHERE c.user_id = ? ")
                .append("GROUP BY c.conversation_id ");

        List<String> args = new ArrayList<>();
        args.add(HistoryMessage.SENT_BY_ME);
        args.add(String.valueOf(Math.max(0, userId)));
        if (startInclusive > 0L) {
            sql.append("HAVING MAX(c.created_at) >= ? ");
            args.add(String.valueOf(startInclusive));
            if (endExclusive > 0L) {
                sql.append("AND MAX(c.created_at) < ? ");
                args.add(String.valueOf(endExclusive));
            }
        } else if (endExclusive > 0L) {
            sql.append("HAVING MAX(c.created_at) < ? ");
            args.add(String.valueOf(endExclusive));
        }
        sql.append("ORDER BY last_time DESC ");
        if (limit > 0) sql.append("LIMIT ").append(Math.min(100, limit));

        try (Cursor cursor = getReadableDatabase().rawQuery(
                sql.toString(), args.toArray(new String[0]))) {
            long rowId = 1;
            while (cursor.moveToNext()) {
                String conversationId = cursor.getString(0);
                long lastTime = cursor.getLong(1);
                String firstUser = cursor.isNull(2) ? "Cuộc trò chuyện Lịch sử" : cursor.getString(2);
                String lastContent = cursor.isNull(3) ? "" : cursor.getString(3);
                result.add(new HistoryHistoryItem(
                        rowId++,
                        conversationId,
                        shorten(cleanImageMarker(firstUser), 56),
                        shorten(cleanImageMarker(lastContent), 120),
                        String.valueOf(lastTime)
                ));
            }
        }
        return result;
    }

    public int deleteConversation(int userId, String conversationId) {
        return getWritableDatabase().delete(
                "chat_messages",
                "user_id = ? AND conversation_id = ?",
                new String[]{String.valueOf(Math.max(0, userId)), normalizeConversationId(conversationId)}
        );
    }

    public long insertNote(int userId, String title, String content) {
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("user_id", Math.max(0, userId));
        values.put("title", safeText(title));
        values.put("content", safeText(content));
        values.put("category", "Ghi chú");
        values.put("pinned", 0);
        values.put("created_at", now);
        values.put("updated_at", now);
        return getWritableDatabase().insert("note_items", null, values);
    }

    public int updateNote(int userId, long noteId, String title, String content) {
        ContentValues values = new ContentValues();
        values.put("title", safeText(title));
        values.put("content", safeText(content));
        values.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().update(
                "note_items",
                values,
                "id = ? AND user_id = ?",
                new String[]{String.valueOf(noteId), String.valueOf(Math.max(0, userId))}
        );
    }

    public int deleteNote(int userId, long noteId) {
        return getWritableDatabase().delete(
                "note_items",
                "id = ? AND user_id = ?",
                new String[]{String.valueOf(noteId), String.valueOf(Math.max(0, userId))}
        );
    }

    public int setNotePinned(int userId, long noteId, boolean pinned) {
        ContentValues values = new ContentValues();
        values.put("pinned", pinned ? 1 : 0);
        values.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().update(
                "note_items",
                values,
                "id = ? AND user_id = ?",
                new String[]{String.valueOf(noteId), String.valueOf(Math.max(0, userId))}
        );
    }

    public List<HistoryNoteItem> loadNotes(int userId) {
        List<HistoryNoteItem> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "note_items",
                new String[]{"id", "title", "content", "pinned", "updated_at"},
                "user_id = ?",
                new String[]{String.valueOf(Math.max(0, userId))},
                null,
                null,
                "pinned DESC, updated_at DESC, id DESC"
        )) {
            while (cursor.moveToNext()) {
                result.add(new HistoryNoteItem(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getInt(3) == 1,
                        cursor.getLong(4)
                ));
            }
        }
        return result;
    }

    public long saveQuizAttempt(int userId,
                                String topic,
                                String educationLevel,
                                int correct,
                                int total,
                                int expEarned) {
        ContentValues values = new ContentValues();
        values.put("user_id", Math.max(0, userId));
        values.put("topic", safeText(topic).isEmpty() ? "Tổng hợp" : safeText(topic));
        values.put("education_level", safeText(educationLevel).isEmpty()
                ? "Trung học cơ sở" : safeText(educationLevel));
        values.put("correct_count", Math.max(0, correct));
        values.put("total_count", Math.max(0, total));
        values.put("exp_earned", Math.max(0, expEarned));
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("quiz_attempts", null, values);
    }

    public int getTotalQuizExp(int userId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(exp_earned), 0) FROM quiz_attempts WHERE user_id = ?",
                new String[]{String.valueOf(Math.max(0, userId))})) {
            return cursor.moveToFirst() ? Math.max(0, cursor.getInt(0)) : 0;
        }
    }

    public List<HistoryHistoryItem> loadQuizAttempts(int userId, int limit) {
        List<HistoryHistoryItem> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "quiz_attempts",
                new String[]{"id", "topic", "education_level", "correct_count",
                        "total_count", "exp_earned", "created_at"},
                "user_id = ?",
                new String[]{String.valueOf(Math.max(0, userId))},
                null,
                null,
                "created_at DESC, id DESC",
                limit > 0 ? String.valueOf(Math.min(100, limit)) : null
        )) {
            while (cursor.moveToNext()) {
                int correct = cursor.getInt(3);
                int total = cursor.getInt(4);
                int percent = total <= 0 ? 0 : Math.round(correct * 100f / total);
                result.add(new HistoryHistoryItem(
                        cursor.getLong(0),
                        cursor.getString(1) + " • " + cursor.getString(2),
                        correct + "/" + total + " câu đúng • " + percent
                                + "% chính xác • +" + cursor.getInt(5) + " EXP",
                        String.valueOf(cursor.getLong(6))
                ));
            }
        }
        return result;
    }

    private String normalizeConversationId(String value) {
        return value == null || value.trim().isEmpty() ? "default" : value.trim();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanImageMarker(String value) {
        return safeText(value).replace("[Đã gửi kèm ảnh]", "").trim();
    }

    private String shorten(String value, int maxLength) {
        String safe = safeText(value).replaceAll("\\s+", " ");
        return safe.length() <= maxLength ? safe
                : safe.substring(0, Math.max(1, maxLength - 1)) + "…";
    }
}
