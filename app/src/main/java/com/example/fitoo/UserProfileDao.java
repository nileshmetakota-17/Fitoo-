package com.example.fitoo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    UserProfile get();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserProfile profile);

    @Update
    void update(UserProfile profile);
}
