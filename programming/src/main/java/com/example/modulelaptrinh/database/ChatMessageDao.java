package com.example.modulelaptrinh.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.modulelaptrinh.models.ChatMessage;
import java.util.List;

@Dao
public interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    List<ChatMessage> getAllMessages();

    @Query("SELECT * FROM chat_messages WHERE language = :language ORDER BY timestamp DESC")
    List<ChatMessage> getMessagesByLanguage(String language);

    @Insert
    void insertMessage(ChatMessage message);

    @Query("DELETE FROM chat_messages")
    void deleteAllMessages();

    @Query("DELETE FROM chat_messages WHERE id = :id")
    void deleteMessage(long id);
}