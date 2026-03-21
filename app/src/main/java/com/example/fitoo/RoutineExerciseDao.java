package com.example.fitoo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RoutineExerciseDao {

    @Insert
    void insert(RoutineExercise exercise);

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sortOrder")
    List<RoutineExercise> getByRoutineId(int routineId);
}
