package com.example.asm_final.chat;

import java.util.*;

/** Immutable account-owned UI records; no credentials or public file paths. */
public final class ChatRecords {
  private ChatRecords() {}

  public static final class Folder {
    public final String id, title;

    public Folder(String id, String title) {
      this.id = id;
      this.title = title;
    }
  }

  public static final class Thread {
    public final String id, title, folderId, draft;
    public final boolean automaticTitle;
    public final long updatedAt;

    public Thread(
        String id,
        String title,
        String folderId,
        String draft,
        boolean automaticTitle,
        long updatedAt) {
      this.id = id;
      this.title = title;
      this.folderId = folderId;
      this.draft = draft;
      this.automaticTitle = automaticTitle;
      this.updatedAt = updatedAt;
    }
  }

  public static final class FileItem {
    public final String id, name, mime, storageKey;
    public final int size;

    public FileItem(String id, String name, String mime, String storageKey, int size) {
      this.id = id;
      this.name = name;
      this.mime = mime;
      this.storageKey = storageKey;
      this.size = size;
    }
  }

  public static final class Message {
    public final String id, text, role, status, error;
    public final boolean truncated;
    public final List<FileItem> files;

    public Message(
        String id,
        String text,
        String role,
        String status,
        String error,
        boolean truncated,
        List<FileItem> files) {
      this.id = id;
      this.text = text;
      this.role = role;
      this.status = status;
      this.error = error;
      this.truncated = truncated;
      this.files = Collections.unmodifiableList(new ArrayList<>(files));
    }
  }

  public static final class Snapshot {
    public final Thread thread;
    public final List<Message> messages;
    public final List<FileItem> draftFiles;

    public Snapshot(Thread thread, List<Message> messages, List<FileItem> draftFiles) {
      this.thread = thread;
      this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
      this.draftFiles = Collections.unmodifiableList(new ArrayList<>(draftFiles));
    }
  }
}
