package com.example.fitoo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WorkoutLogDao {

    @Insert
    void insert(WorkoutLogEntry entry);

    @Update
    void update(WorkoutLogEntry entry);

    @Query("DELETE FROM workout_log WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM workout_log WHERE id = :id LIMIT 1")
    WorkoutLogEntry getById(int id);

    @Query("SELECT * FROM workout_log WHERE dateDay = :day ORDER BY muscleGroup, id")
    List<WorkoutLogEntry> getByDay(long day);

    @Query("SELECT DISTINCT dateDay FROM workout_log ORDER BY dateDay DESC LIMIT 30")
    List<Long> getRecentDays();

    @Query("UPDATE workout_log SET muscleGroup = :replacement WHERE muscleGroup = :target")
    void replaceMuscleGroup(String target, String replacement);

    @Query("DELETE FROM workout_log WHERE dateDay = :day")
    void deleteByDay(long day);

    @Query("DELETE FROM workout_log")
    void deleteAll();
}
