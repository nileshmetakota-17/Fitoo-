package com.example.fitoo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_routines")
public class WorkoutRoutine {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String description;
}
