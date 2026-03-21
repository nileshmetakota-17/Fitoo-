package com.example.fitoo;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DietPlanDao {

    @Insert
    void insert(DietPlanItem item);

    @Update
    void update(DietPlanItem item);

    @Delete
    void delete(DietPlanItem item);

    @Query("SELECT * FROM diet_plan ORDER BY sortOrder, id")
    List<DietPlanItem> getAll();

    @Query("DELETE FROM diet_plan")
    void deleteAll();
}
