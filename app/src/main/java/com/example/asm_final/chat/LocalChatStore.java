package com.example.asm_final.chat;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import com.example.mentor.chat.*;
import com.example.mentor.learning.LearningContext;
import java.util.*;

/** Synchronized local persistence. Every operation checks ownership even when passed a known ID. */
public final class LocalChatStore implements ChatStore {
  private final Context context;
  private final LocalChatFiles files;

  public LocalChatStore(Context context) {
    this.context = context.getApplicationContext();
    this.files = new LocalChatFiles(this.context);
  }

  private String[] args(int owner, String id) {
    if (owner <= 0 || id == null || id.isEmpty())
      throw new IllegalArgumentException("Missing chat ownership");
    return new String[] {id, Integer.toString(owner)};
  }

  private String title(String title, int max) {
    if (title == null || title.trim().isEmpty() || title.trim().length() > max)
      throw new IllegalArgumentException("Invalid title");
    return title.trim();
  }

  private void owned(SQLiteDatabase db, int owner, String id, String table) {
    try (Cursor c =
        db.rawQuery("SELECT 1 FROM " + table + " WHERE id=? AND user_id=?", args(owner, id))) {
      if (!c.moveToFirst())
        throw new IllegalArgumentException("Chat resource does not belong to account");
    }
  }

  private ChatRecords.Thread readThread(Cursor c) {
    return new ChatRecords.Thread(
        c.getString(0),
        c.getString(1),
        c.getString(2),
        c.getString(3),
        c.getInt(4) != 0,
        c.getLong(5));
  }

  private List<ChatRecords.FileItem> readFiles(
      SQLiteDatabase db, int owner, String threadId, String messageId) {
    List<ChatRecords.FileItem> result = new ArrayList<>();
    String where = messageId == null ? "message_id IS NULL" : "message_id=?";
    String[] params =
        messageId == null
            ? new String[] {threadId, Integer.toString(owner)}
            : new String[] {threadId, Integer.toString(owner), messageId};
    try (Cursor c =
        db.rawQuery(
            "SELECT id,name,mime,storage_key,size FROM mentor_chat_files WHERE thread_id=? AND"
                + " user_id=? AND "
                + where
                + " ORDER BY rowid",
            params)) {
      while (c.moveToNext())
        result.add(
            new ChatRecords.FileItem(
                c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4)));
    }
    return result;
  }

  @Override
  public synchronized ChatRecords.Thread create(int owner, String folderId) {
    if (owner <= 0) throw new IllegalArgumentException("Missing owner");
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      if (folderId != null) owned(db, owner, folderId, "mentor_chat_folders");
      ContentValues v = new ContentValues();
      String id = UUID.randomUUID().toString();
      v.put("id", id);
      v.put("user_id", owner);
      v.put("folder_id", folderId);
      v.put("updated_at", System.currentTimeMillis());
      db.insertOrThrow("mentor_chat_threads", null, v);
      return thread(owner, id);
    }
  }

  @Override
  public synchronized ChatRecords.Thread thread(int owner, String id) {
    try (ChatDatabase h = new ChatDatabase(context);
        Cursor c =
            h.getReadableDatabase()
                .rawQuery(
                    "SELECT id,title,folder_id,draft,automatic_title,updated_at FROM"
                        + " mentor_chat_threads WHERE id=? AND user_id=?",
                    args(owner, id))) {
      if (!c.moveToFirst()) throw new IllegalArgumentException("Missing owned chat");
      return readThread(c);
    }
  }

  @Override
  public synchronized List<ChatRecords.Thread> threads(int owner, String query) {
    if (owner <= 0) throw new IllegalArgumentException("Missing owner");
    List<ChatRecords.Thread> result = new ArrayList<>();
    // instr uses literal search: %, _ and apostrophes never become wildcard/SQL operators.
    String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    try (ChatDatabase h = new ChatDatabase(context);
        Cursor c =
            h.getReadableDatabase()
                .rawQuery(
                    "SELECT id,title,folder_id,draft,automatic_title,updated_at FROM"
                        + " mentor_chat_threads WHERE user_id=? AND (?='' OR"
                        + " instr(lower(title),?)>0 OR instr(lower(draft),?)>0) ORDER BY updated_at"
                        + " DESC,id",
                    new String[] {Integer.toString(owner), q, q, q})) {
      while (c.moveToNext()) result.add(readThread(c));
    }
    return result;
  }

  @Override
  public synchronized ChatRecords.Snapshot snapshot(int owner, String id) {
    ChatRecords.Thread thread = thread(owner, id);
    List<ChatRecords.Message> messages = new ArrayList<>();
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getReadableDatabase();
      try (Cursor c =
          db.rawQuery(
              "SELECT id,text,role,status,error,truncated FROM mentor_chat_messages WHERE"
                  + " thread_id=? AND user_id=? ORDER BY turn_index,CASE role WHEN 'USER' THEN 0"
                  + " ELSE 1 END",
              args(owner, id))) {
        while (c.moveToNext())
          messages.add(
              new ChatRecords.Message(
                  c.getString(0),
                  c.getString(1),
                  c.getString(2),
                  c.getString(3),
                  c.getString(4),
                  c.getInt(5) != 0,
                  readFiles(db, owner, id, c.getString(0))));
      }
      return new ChatRecords.Snapshot(thread, messages, readFiles(db, owner, id, null));
    }
  }

  @Override
  public synchronized List<ChatRecords.Folder> folders(int owner) {
    List<ChatRecords.Folder> result = new ArrayList<>();
    try (ChatDatabase h = new ChatDatabase(context);
        Cursor c =
            h.getReadableDatabase()
                .rawQuery(
                    "SELECT id,title FROM mentor_chat_folders WHERE user_id=? ORDER BY title,id",
                    new String[] {Integer.toString(owner)})) {
      while (c.moveToNext()) result.add(new ChatRecords.Folder(c.getString(0), c.getString(1)));
    }
    return result;
  }

  @Override
  public synchronized String createFolder(int owner, String label) {
    if (owner <= 0) throw new IllegalArgumentException("Missing owner");
    try (ChatDatabase h = new ChatDatabase(context)) {
      String id = UUID.randomUUID().toString();
      ContentValues v = new ContentValues();
      v.put("id", id);
      v.put("user_id", owner);
      v.put("title", title(label, 100));
      h.getWritableDatabase().insertOrThrow("mentor_chat_folders", null, v);
      return id;
    }
  }

  @Override
  public synchronized void renameFolder(int owner, String id, String label) {
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      owned(db, owner, id, "mentor_chat_folders");
      ContentValues v = new ContentValues();
      v.put("title", title(label, 100));
      db.update("mentor_chat_folders", v, "id=? AND user_id=?", args(owner, id));
    }
  }

  @Override
  public synchronized void renameThread(int owner, String id, String label) {
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      owned(db, owner, id, "mentor_chat_threads");
      ContentValues v = new ContentValues();
      v.put("title", title(label, 120));
      v.put("automatic_title", 0);
      db.update("mentor_chat_threads", v, "id=? AND user_id=?", args(owner, id));
    }
  }

  @Override
  public synchronized void moveThread(int owner, String id, String folderId) {
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      owned(db, owner, id, "mentor_chat_threads");
      if (folderId != null) owned(db, owner, folderId, "mentor_chat_folders");
      ContentValues v = new ContentValues();
      v.put("folder_id", folderId);
      db.update("mentor_chat_threads", v, "id=? AND user_id=?", args(owner, id));
    }
  }

  private List<ChatRecords.FileItem> allFiles(
      SQLiteDatabase db, int owner, String where, String value) {
    List<ChatRecords.FileItem> result = new ArrayList<>();
    try (Cursor c =
        db.rawQuery(
            "SELECT id,name,mime,storage_key,size FROM mentor_chat_files WHERE user_id=? AND "
                + where
                + "=?",
            new String[] {Integer.toString(owner), value})) {
      while (c.moveToNext())
        result.add(
            new ChatRecords.FileItem(
                c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4)));
    }
    return result;
  }

  @Override
  public synchronized void deleteThread(int owner, String id) {
    List<ChatRecords.FileItem> removed;
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      owned(db, owner, id, "mentor_chat_threads");
      removed = allFiles(db, owner, "thread_id", id);
      db.delete("mentor_chat_threads", "id=? AND user_id=?", args(owner, id));
    }
    for (ChatRecords.FileItem file : removed) files.delete(owner, file);
  }

  @Override
  public synchronized void deleteFolder(int owner, String id, boolean deleteChats) {
    List<ChatRecords.FileItem> removed = new ArrayList<>();
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      db.beginTransaction();
      try {
        owned(db, owner, id, "mentor_chat_folders");
        if (deleteChats) {
          try (Cursor c =
              db.rawQuery(
                  "SELECT id FROM mentor_chat_threads WHERE folder_id=? AND user_id=?",
                  args(owner, id))) {
            while (c.moveToNext()) removed.addAll(allFiles(db, owner, "thread_id", c.getString(0)));
          }
          db.delete("mentor_chat_threads", "folder_id=? AND user_id=?", args(owner, id));
        } else {
          ContentValues v = new ContentValues();
          v.putNull("folder_id");
          db.update("mentor_chat_threads", v, "folder_id=? AND user_id=?", args(owner, id));
        }
        db.delete("mentor_chat_folders", "id=? AND user_id=?", args(owner, id));
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
    }
    for (ChatRecords.FileItem file : removed) files.delete(owner, file);
  }

  @Override
  public synchronized void saveDraft(int owner, String id, String text) {
    if (text == null || text.length() > 30000) throw new IllegalArgumentException("Draft too long");
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      owned(db, owner, id, "mentor_chat_threads");
      ContentValues v = new ContentValues();
      v.put("draft", text);
      db.update("mentor_chat_threads", v, "id=? AND user_id=?", args(owner, id));
    }
  }

  @Override
  public synchronized void addDraftFile(int owner, String threadId, ChatRecords.FileItem file) {
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      owned(db, owner, threadId, "mentor_chat_threads");
      if (readFiles(db, owner, threadId, null).size() >= 4)
        throw new IllegalArgumentException("Too many attachments");
      files.resolve(owner, file); // validates storage key and account-scoped path
      ContentValues v = new ContentValues();
      v.put("id", file.id);
      v.put("user_id", owner);
      v.put("thread_id", threadId);
      v.put("name", file.name);
      v.put("mime", file.mime);
      v.put("storage_key", file.storageKey);
      v.put("size", file.size);
      db.insertOrThrow("mentor_chat_files", null, v);
    }
  }

  @Override
  public synchronized void removeDraftFile(int owner, String threadId, String fileId) {
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      owned(db, owner, threadId, "mentor_chat_threads");
      for (ChatRecords.FileItem f : readFiles(db, owner, threadId, null))
        if (f.id.equals(fileId)) {
          db.delete(
              "mentor_chat_files",
              "id=? AND user_id=? AND message_id IS NULL",
              args(owner, fileId));
          files.delete(owner, f);
          return;
        }
      throw new IllegalArgumentException("Missing owned draft attachment");
    }
  }

  @Override
  public synchronized String stageMessage(int owner, String threadId, String text) {
    if (text == null || text.length() > 30000)
      throw new IllegalArgumentException("Invalid question");
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      db.beginTransaction();
      try {
        owned(db, owner, threadId, "mentor_chat_threads");
        if (text.trim().isEmpty() && readFiles(db, owner, threadId, null).isEmpty())
          throw new IllegalArgumentException("Empty question");
        int next = 0;
        try (Cursor c =
            db.rawQuery(
                "SELECT COALESCE(MAX(turn_index),0)+1 FROM mentor_chat_messages WHERE thread_id=?"
                    + " AND user_id=?",
                args(owner, threadId))) {
          c.moveToFirst();
          next = c.getInt(0);
        }
        String messageId = UUID.randomUUID().toString();
        ContentValues v = new ContentValues();
        v.put("id", messageId);
        v.put("user_id", owner);
        v.put("thread_id", threadId);
        v.put("turn_index", next);
        v.put("role", "USER");
        v.put("text", text);
        v.put("status", "PENDING");
        db.insertOrThrow("mentor_chat_messages", null, v);
        ContentValues attachment = new ContentValues();
        attachment.put("message_id", messageId);
        db.update(
            "mentor_chat_files",
            attachment,
            "thread_id=? AND user_id=? AND message_id IS NULL",
            args(owner, threadId));
        ContentValues thread = new ContentValues();
        thread.put("draft", "");
        thread.put("updated_at", System.currentTimeMillis());
        db.update("mentor_chat_threads", thread, "id=? AND user_id=?", args(owner, threadId));
        String automatic = text.trim().replaceAll("\\s+", " ");
        if (automatic.isEmpty()) automatic = readFiles(db, owner, threadId, messageId).get(0).name;
        thread.clear();
        thread.put("title", automatic.substring(0, Math.min(70, automatic.length())));
        db.update(
            "mentor_chat_threads",
            thread,
            "id=? AND user_id=? AND automatic_title=1 AND title=''",
            args(owner, threadId));
        db.setTransactionSuccessful();
        return messageId;
      } finally {
        db.endTransaction();
      }
    }
  }

  private List<TutorAttachment> payloads(int owner, List<ChatRecords.FileItem> items) {
    List<TutorAttachment> result = new ArrayList<>();
    for (ChatRecords.FileItem f : items) result.add(files.payload(owner, f));
    return result;
  }

  @Override
  public synchronized TutorRequest request(
      int owner, String threadId, String messageId, LearningContext context) {
    if (context.userId != owner) throw new IllegalArgumentException("Wrong request owner");
    ChatRecords.Snapshot snap = snapshot(owner, threadId);
    List<TutorTurn> turns = new ArrayList<>();
    for (int i = 0; i < snap.messages.size(); i++) {
      ChatRecords.Message m = snap.messages.get(i);
      if (!m.role.equals("USER")) continue;
      if (m.id.equals(messageId)) {
        turns.add(new TutorTurn(owner, TutorTurn.Role.USER, m.text, payloads(owner, m.files)));
        return new TutorRequest(context, turns, null);
      }
      if (m.status.equals("SENT")
          && i + 1 < snap.messages.size()
          && snap.messages.get(i + 1).role.equals("ASSISTANT")) {
        turns.add(new TutorTurn(owner, TutorTurn.Role.USER, m.text, payloads(owner, m.files)));
        turns.add(
            new TutorTurn(
                owner,
                TutorTurn.Role.ASSISTANT,
                snap.messages.get(i + 1).text,
                Collections.emptyList()));
      }
    }
    throw new IllegalArgumentException("Missing owned question");
  }

  @Override
  public synchronized void markPending(int owner, String threadId, String messageId) {
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      owned(db, owner, threadId, "mentor_chat_threads");
      ContentValues v = new ContentValues();
      v.put("status", "PENDING");
      v.put("error", "");
      int changed =
          db.update(
              "mentor_chat_messages",
              v,
              "id=? AND user_id=? AND thread_id=? AND role='USER' AND status IN"
                  + " ('FAILED','CANCELLED')",
              new String[] {messageId, Integer.toString(owner), threadId});
      if (changed != 1) throw new IllegalArgumentException("Question cannot be retried");
    }
  }

  @Override
  public synchronized boolean finish(
      int owner, String threadId, String messageId, TutorReply reply, String error) {
    try (ChatDatabase h = new ChatDatabase(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      db.beginTransaction();
      try {
        int turn;
        try (Cursor c =
            db.rawQuery(
                "SELECT turn_index FROM mentor_chat_messages WHERE id=? AND user_id=? AND"
                    + " thread_id=? AND role='USER' AND status='PENDING'",
                new String[] {messageId, Integer.toString(owner), threadId})) {
          if (!c.moveToFirst()) return false;
          turn = c.getInt(0);
        }
        ContentValues v = new ContentValues();
        v.put(
            "status", reply != null ? "SENT" : "CANCELLED".equals(error) ? "CANCELLED" : "FAILED");
        v.put("error", error == null ? "" : error);
        db.update("mentor_chat_messages", v, "id=? AND user_id=?", args(owner, messageId));
        if (reply != null) {
          v.clear();
          v.put("id", UUID.randomUUID().toString());
          v.put("user_id", owner);
          v.put("thread_id", threadId);
          v.put("turn_index", turn);
          v.put("role", "ASSISTANT");
          v.put("text", reply.text);
          v.put("status", "SENT");
          v.put("truncated", reply.truncated ? 1 : 0);
          db.insertOrThrow("mentor_chat_messages", null, v);
        }
        v.clear();
        v.put("updated_at", System.currentTimeMillis());
        db.update("mentor_chat_threads", v, "id=? AND user_id=?", args(owner, threadId));
        db.setTransactionSuccessful();
        return true;
      } finally {
        db.endTransaction();
      }
    }
  }

  @Override
  public synchronized void recoverInterrupted(int owner) {
    try (ChatDatabase h = new ChatDatabase(context)) {
      ContentValues v = new ContentValues();
      v.put("status", "CANCELLED");
      v.put("error", "CANCELLED");
      h.getWritableDatabase()
          .update(
              "mentor_chat_messages",
              v,
              "user_id=? AND status='PENDING'",
              new String[] {Integer.toString(owner)});
    }
  }
}
