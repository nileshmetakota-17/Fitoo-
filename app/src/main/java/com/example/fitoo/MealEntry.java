package com.example.fitoo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "meals")
public class MealEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String category; // Breakfast, Lunch, Snacks, Dinner
    public float quantity;  // count (e.g. 6) or grams (e.g. 250)
    /** "count" = pieces, "g" = weight in grams */
    public String quantityUnit;

    public float calories;
    public float protein;
    public float carbs;
    public float fats;
    public float fiber;

    public long timestamp;  // epoch millis
    /** true when user has marked this meal as eaten */
    public boolean eaten;
}

