package com.osmino.sova.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.osmino.sova.model.Assistant;

import java.util.List;

@Dao
public interface AssistantDao {
    @Query("SELECT * FROM assistants WHERE id = :assistantId")
    Assistant getAssistant(long assistantId);

    @Query("SELECT * FROM assistants")
    LiveData<List<Assistant>> getAll();

    @Query("SELECT COUNT(id) FROM assistants")
    int getCount();

    @Query("SELECT id FROM assistants ORDER BY ROWID ASC LIMIT 1")
    int getFirstAssistantId();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Assistant assistant);

    @Update
    void update(Assistant assistant);

    @Query("DELETE FROM assistants WHERE id = :id")
    void delete(long id);


    @Query("UPDATE assistants SET cuid = :cuid WHERE id = :assistantId")
    void saveCUID(long assistantId, String cuid);
}
