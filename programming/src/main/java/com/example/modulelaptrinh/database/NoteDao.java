package com.example.modulelaptrinh.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.modulelaptrinh.models.Note;
import java.util.List;

@Dao
public interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY date DESC")
    List<Note> getAllNotes();

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    List<Note> searchNotes(String query);

    @Insert
    void insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("DELETE FROM notes")
    void deleteAllNotes();
}