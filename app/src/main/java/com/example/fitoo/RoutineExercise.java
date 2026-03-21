package com.example.fitoo;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "routine_exercises",
        foreignKeys = @ForeignKey(
                entity = WorkoutRoutine.class,
                parentColumns = "id",
                childColumns = "routineId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("routineId")
)
public class RoutineExercise {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int routineId;
    public String name;
    public String setsReps;  // e.g. "3x12" or "2 min"
    public int sortOrder;
}
