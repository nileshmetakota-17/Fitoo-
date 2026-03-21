package com.example.fitoo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_log")
public class WorkoutLogEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Day in millis (start of day) for grouping */
    public long dateDay;
    /** Chest, Back, Triceps, Biceps, Legs, Shoulders */
    public String muscleGroup;
    public String exerciseName;
    public int sets;
    public int reps;
    /** true when user has marked this exercise as completed */
    public boolean completed;
}
