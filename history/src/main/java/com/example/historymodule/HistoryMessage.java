package com.example.historymodule;

public final class HistoryMessage {
    public static final String SENT_BY_ME = "me";
    public static final String SENT_BY_BOT = "bot";

    private String message;
    private final String sentBy;
    private boolean pending;
    private final boolean imageAttached;

    public HistoryMessage(String message, String sentBy) {
        this(message, sentBy, false, false);
    }

    public HistoryMessage(String message,
                          String sentBy,
                          boolean pending,
                          boolean imageAttached) {
        this.message = message == null ? "" : message;
        this.sentBy = sentBy == null ? SENT_BY_BOT : sentBy;
        this.pending = pending;
        this.imageAttached = imageAttached;
    }

    public static HistoryMessage pending() {
        return new HistoryMessage("AI đang phân tích...", SENT_BY_BOT, true, false);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }

    public String getSentBy() {
        return sentBy;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    public boolean hasImageAttached() {
        return imageAttached;
    }
}
