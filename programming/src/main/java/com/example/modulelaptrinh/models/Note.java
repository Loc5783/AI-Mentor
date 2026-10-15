package com.example.modulelaptrinh.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.modulelaptrinh.utils.DateConverter;
import java.util.Date;

@Entity(tableName = "notes")
@TypeConverters(DateConverter.class)
public class Note {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String content;
    private Date date;
    private boolean isFavorite;

    public Note() {
    }

    @Ignore
    public Note(String title, String content, Date date) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.isFavorite = false;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}