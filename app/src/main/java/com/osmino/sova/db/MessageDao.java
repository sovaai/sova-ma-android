package com.osmino.sova.db;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.osmino.sova.model.chat.Message;

import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages WHERE assistantId = :assistantId")
    DataSource.Factory<Integer, Message> getAll(int assistantId);

    @Query("SELECT * FROM messages WHERE assistantId = :assistantId")
    List<Message> getAllList(int assistantId);

    @Query("SELECT COUNT(id) FROM messages WHERE assistantId = :assistantId")
    int getMessagesCount(int assistantId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<Message> messages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Message message);

//    @Query("SELECT * FROM messages WHERE id IN (:userIds)")
//    List<Message> loadAllByIds(int[] userIds);

//    @Query("SELECT * FROM message WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    Message findByName(String first, String last);

    @Insert
    void insertAll(Message... msgs);

    @Delete
    void delete(Message msg);

    @Query("DELETE FROM messages WHERE assistantId = :assistantId")
    void deleteAll(int assistantId);
}
