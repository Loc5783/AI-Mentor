package com.example.mathmodule;
public class MathMessage {
    private String content;
    private boolean isUser; // true nếu là học sinh gửi, false nếu là AI trả lời

    public MathMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
    }

    public String getContent() {
        return content;
    }

    public boolean isUser() {
        return isUser;
    }
}
