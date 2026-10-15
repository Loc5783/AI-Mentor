package com.example.modulelaptrinh.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.example.modulelaptrinh.models.ChatMessage;
import com.example.modulelaptrinh.models.Note;
import com.example.modulelaptrinh.utils.DateConverter;

@Database(entities = {Note.class, ChatMessage.class}, version = 1, exportSchema = false)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract NoteDao noteDao();
    public abstract ChatMessageDao chatMessageDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "ai_mentor_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}