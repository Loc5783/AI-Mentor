package com.example.mathmodule.databases;

public class MathHistoryItem {
    private int id;
    private String question;
    private String answer;
    private String timestamp;

    public MathHistoryItem() {
    }

    public MathHistoryItem(String question, String answer, String timestamp) {
        this.question = question;
        this.answer = answer;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}