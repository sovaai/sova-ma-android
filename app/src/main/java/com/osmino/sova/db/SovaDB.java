package com.osmino.sova.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.osmino.sova.model.Assistant;
import com.osmino.sova.model.chat.Message;

@Database(
        entities = {Message.class, Assistant.class},
        version = 10,
        exportSchema = false
)
public abstract class SovaDB extends RoomDatabase {
    public static SovaDB create(Context context) {
        return Room
                .databaseBuilder(context, SovaDB.class, "sova_db")
                .fallbackToDestructiveMigration()
                .build();
    }

    public abstract MessageDao messageDao();
    public abstract AssistantDao assistantDao();
}
