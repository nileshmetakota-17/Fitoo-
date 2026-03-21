package com.example.fitoo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {

    @PrimaryKey
    public int id = 1;

    public String photoUri;
    public String name;
    public String gender;
    public int age;
    public float heightCm;
    public float weightKg;
    public String fitnessGoal;   // e.g. Weight Loss, Muscle Gain, Maintain
    public float targetCalories; // daily calorie goal
    public float targetProtein;  // daily protein goal in grams
}
