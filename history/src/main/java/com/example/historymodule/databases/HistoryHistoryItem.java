package com.example.historymodule.databases;

/** Display model used for conversation summaries and quiz attempt summaries. */
public final class HistoryHistoryItem {
    private final long id;
    private final String referenceId;
    private final String title;
    private final String content;
    private final String timestamp;

    public HistoryHistoryItem(long id, String title, String content, String timestamp) {
        this(id, null, title, content, timestamp);
    }

    public HistoryHistoryItem(long id,
                              String referenceId,
                              String title,
                              String content,
                              String timestamp) {
        this.id = id;
        this.referenceId = referenceId;
        this.title = title == null ? "" : title;
        this.content = content == null ? "" : content;
        this.timestamp = timestamp == null ? "" : timestamp;
    }

    public long getId() { return id; }
    public String getReferenceId() { return referenceId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
}
