package com.example.asm_final.chat;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.example.mentor.data.ModuleDatabaseHelper;
import java.util.*;

/** Adds namespaced tables to AImentor.db without replacing legacy subject tables. */
public final class ChatDatabase extends ModuleDatabaseHelper {
  public ChatDatabase(Context context) {
    super(context, "general_chat", null, 1);
  }

  @Override
  protected Map<String, String> legacyTables() {
    return Collections.emptyMap();
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE mentor_chat_folders(id TEXT PRIMARY KEY,user_id INTEGER NOT NULL REFERENCES"
            + " users(id) ON DELETE CASCADE,title TEXT NOT NULL,UNIQUE(id,user_id))");
    db.execSQL(
        "CREATE TABLE mentor_chat_threads(id TEXT PRIMARY KEY,user_id INTEGER NOT NULL REFERENCES"
            + " users(id) ON DELETE CASCADE,folder_id TEXT,title TEXT NOT NULL DEFAULT"
            + " '',automatic_title INTEGER NOT NULL DEFAULT 1,draft TEXT NOT NULL DEFAULT"
            + " '',updated_at INTEGER NOT NULL,UNIQUE(id,user_id),FOREIGN KEY(folder_id,user_id)"
            + " REFERENCES mentor_chat_folders(id,user_id))");
    db.execSQL(
        "CREATE TABLE mentor_chat_messages(id TEXT PRIMARY KEY,user_id INTEGER NOT NULL,thread_id"
            + " TEXT NOT NULL,turn_index INTEGER NOT NULL,role TEXT NOT NULL CHECK(role IN"
            + " ('USER','ASSISTANT')),text TEXT NOT NULL,status TEXT NOT NULL,error TEXT NOT NULL"
            + " DEFAULT '',truncated INTEGER NOT NULL DEFAULT"
            + " 0,UNIQUE(id,user_id),UNIQUE(thread_id,turn_index,role),FOREIGN"
            + " KEY(thread_id,user_id) REFERENCES mentor_chat_threads(id,user_id) ON DELETE"
            + " CASCADE)");
    db.execSQL(
        "CREATE TABLE mentor_chat_files(id TEXT PRIMARY KEY,user_id INTEGER NOT NULL,thread_id TEXT"
            + " NOT NULL,message_id TEXT,name TEXT NOT NULL,mime TEXT NOT NULL,storage_key TEXT NOT"
            + " NULL,size INTEGER NOT NULL,FOREIGN KEY(thread_id,user_id) REFERENCES"
            + " mentor_chat_threads(id,user_id) ON DELETE CASCADE,FOREIGN KEY(message_id,user_id)"
            + " REFERENCES mentor_chat_messages(id,user_id) ON DELETE CASCADE)");
    db.execSQL(
        "CREATE INDEX mentor_chat_thread_owner ON mentor_chat_threads(user_id,updated_at DESC)");
    db.execSQL(
        "CREATE INDEX mentor_chat_message_order ON"
            + " mentor_chat_messages(thread_id,turn_index,role)");
    db.execSQL(
        "CREATE INDEX mentor_chat_file_owner ON mentor_chat_files(user_id,thread_id,message_id)");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    throw new IllegalStateException("No chat schema upgrade is defined");
  }
}
