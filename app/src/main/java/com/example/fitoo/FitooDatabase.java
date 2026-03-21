package com.example.fitoo;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                MealEntry.class,
                WorkoutRoutine.class,
                RoutineExercise.class,
                WorkoutLogEntry.class,
                DietPlanItem.class,
                UserProfile.class
        },
        version = 8
)
public abstract class FitooDatabase extends RoomDatabase {

    private static volatile FitooDatabase INSTANCE;

    public abstract MealDao mealDao();
    public abstract WorkoutRoutineDao workoutRoutineDao();
    public abstract RoutineExerciseDao routineExerciseDao();
    public abstract WorkoutLogDao workoutLogDao();
    public abstract DietPlanDao dietPlanDao();
    public abstract UserProfileDao userProfileDao();

    public static FitooDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (FitooDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    FitooDatabase.class,
                                    "fitoo.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
