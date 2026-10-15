package com.example.asm_final.utils;

public class RecentActivity {
    public enum Type { CHAT, QUIZ }
    public enum Subject { MATH, SCIENCE, HISTORY, PROGRAMMING }

    private final Subject subject;
    private final Type type;
    private final String title;
    private final String description;
    private final long timestamp;

    public RecentActivity(Subject subject, Type type, String title, String description, long timestamp) {
        this.subject = subject;
        this.type = type;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
    }

    public Subject getSubject() { return subject; }
    public Type getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getTimestamp() { return timestamp; }
}
