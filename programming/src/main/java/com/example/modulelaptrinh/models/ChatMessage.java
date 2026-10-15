package com.example.modulelaptrinh.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.modulelaptrinh.utils.DateConverter;
import java.util.Date;

@Entity(tableName = "chat_messages")
@TypeConverters(DateConverter.class)
public class ChatMessage {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String question;
    private String answer;
    private Date timestamp;
    private String language;

    public ChatMessage() {
    }

    @Ignore
    public ChatMessage(String question, String answer, Date timestamp, String language) {
        this.question = question;
        this.answer = answer;
        this.timestamp = timestamp;
        this.language = language;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}