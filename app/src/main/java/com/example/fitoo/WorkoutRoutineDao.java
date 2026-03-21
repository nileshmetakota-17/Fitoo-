package com.example.fitoo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WorkoutRoutineDao {

    @Insert
    long insert(WorkoutRoutine routine);

    @Query("SELECT * FROM workout_routines ORDER BY id")
    List<WorkoutRoutine> getAll();

    @Query("SELECT * FROM workout_routines WHERE id = :id LIMIT 1")
    WorkoutRoutine getById(int id);

    @Query("SELECT COUNT(*) FROM workout_routines")
    int getCount();
}
