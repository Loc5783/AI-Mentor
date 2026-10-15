package com.example.historymodule.databases;

public final class HistoryNoteItem {
    private final long id;
    private final String title;
    private final String content;
    private final boolean pinned;
    private final long updatedAt;

    public HistoryNoteItem(long id,
                           String title,
                           String content,
                           boolean pinned,
                           long updatedAt) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.content = content == null ? "" : content;
        this.pinned = pinned;
        this.updatedAt = Math.max(0L, updatedAt);
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isPinned() { return pinned; }
    public long getUpdatedAt() { return updatedAt; }
}
