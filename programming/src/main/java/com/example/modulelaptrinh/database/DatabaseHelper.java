package com.example.modulelaptrinh.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.modulelaptrinh.models.ChatMessage;
import com.example.modulelaptrinh.models.Note;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ai_mentor.db";
    private static final int DATABASE_VERSION = 1;

    // Bảng Notes
    private static final String TABLE_NOTES = "notes";
    private static final String COLUMN_NOTE_ID = "id";
    private static final String COLUMN_NOTE_TITLE = "title";
    private static final String COLUMN_NOTE_CONTENT = "content";
    private static final String COLUMN_NOTE_DATE = "date";
    private static final String COLUMN_NOTE_FAVORITE = "favorite";

    // Bảng Chat Messages
    private static final String TABLE_CHAT = "chat_messages";
    private static final String COLUMN_CHAT_ID = "id";
    private static final String COLUMN_CHAT_QUESTION = "question";
    private static final String COLUMN_CHAT_ANSWER = "answer";
    private static final String COLUMN_CHAT_TIMESTAMP = "timestamp";
    private static final String COLUMN_CHAT_LANGUAGE = "language";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng notes
        String createNotesTable = "CREATE TABLE " + TABLE_NOTES + "("
                + COLUMN_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NOTE_TITLE + " TEXT, "
                + COLUMN_NOTE_CONTENT + " TEXT, "
                + COLUMN_NOTE_DATE + " INTEGER, "
                + COLUMN_NOTE_FAVORITE + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(createNotesTable);

        // Tạo bảng chat_messages
        String createChatTable = "CREATE TABLE " + TABLE_CHAT + "("
                + COLUMN_CHAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_CHAT_QUESTION + " TEXT, "
                + COLUMN_CHAT_ANSWER + " TEXT, "
                + COLUMN_CHAT_TIMESTAMP + " INTEGER, "
                + COLUMN_CHAT_LANGUAGE + " TEXT"
                + ")";
        db.execSQL(createChatTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
        onCreate(db);
    }

    // ========== NOTES METHODS ==========

    public long insertNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_TITLE, note.getTitle());
        values.put(COLUMN_NOTE_CONTENT, note.getContent());
        values.put(COLUMN_NOTE_DATE, note.getDate().getTime());
        values.put(COLUMN_NOTE_FAVORITE, note.isFavorite() ? 1 : 0);

        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NOTES, null, null, null, null, null, COLUMN_NOTE_DATE + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_NOTE_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_CONTENT)));
                note.setDate(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_NOTE_DATE))));
                note.setFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTE_FAVORITE)) == 1);
                notes.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return notes;
    }

    public List<Note> searchNotes(String query) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_NOTE_TITLE + " LIKE ? OR " + COLUMN_NOTE_CONTENT + " LIKE ?";
        String[] selectionArgs = {"%" + query + "%", "%" + query + "%"};

        Cursor cursor = db.query(TABLE_NOTES, null, selection, selectionArgs, null, null, COLUMN_NOTE_DATE + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_NOTE_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_CONTENT)));
                note.setDate(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_NOTE_DATE))));
                note.setFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTE_FAVORITE)) == 1);
                notes.add(note);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return notes;
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTE_TITLE, note.getTitle());
        values.put(COLUMN_NOTE_CONTENT, note.getContent());
        values.put(COLUMN_NOTE_DATE, note.getDate().getTime());
        values.put(COLUMN_NOTE_FAVORITE, note.isFavorite() ? 1 : 0);

        return db.update(TABLE_NOTES, values, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(note.getId())});
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTES, COLUMN_NOTE_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteAllNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NOTES);
        db.close();
    }

    // ========== CHAT METHODS ==========

    public long insertChatMessage(ChatMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CHAT_QUESTION, message.getQuestion());
        values.put(COLUMN_CHAT_ANSWER, message.getAnswer());
        values.put(COLUMN_CHAT_TIMESTAMP, message.getTimestamp().getTime());
        values.put(COLUMN_CHAT_LANGUAGE, message.getLanguage());

        long id = db.insert(TABLE_CHAT, null, values);
        db.close();
        return id;
    }

    public List<ChatMessage> getAllChatMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CHAT, null, null, null, null, null, COLUMN_CHAT_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                ChatMessage message = new ChatMessage();
                message.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CHAT_ID)));
                message.setQuestion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_QUESTION)));
                message.setAnswer(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_ANSWER)));
                message.setTimestamp(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CHAT_TIMESTAMP))));
                message.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_LANGUAGE)));
                messages.add(message);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return messages;
    }

    public List<ChatMessage> getChatMessagesByLanguage(String language) {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CHAT, null, COLUMN_CHAT_LANGUAGE + " = ?",
                new String[]{language}, null, null, COLUMN_CHAT_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                ChatMessage message = new ChatMessage();
                message.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CHAT_ID)));
                message.setQuestion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_QUESTION)));
                message.setAnswer(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_ANSWER)));
                message.setTimestamp(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CHAT_TIMESTAMP))));
                message.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_LANGUAGE)));
                messages.add(message);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return messages;
    }

    public void deleteChatMessage(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHAT, COLUMN_CHAT_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteAllChatMessages() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_CHAT);
        db.close();
    }
}