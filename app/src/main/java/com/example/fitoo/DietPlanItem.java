package com.example.fitoo;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Template meal that appears in "My Diet Plan" and can be applied daily. */
@Entity(tableName = "diet_plan")
public class DietPlanItem {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String category; // Breakfast, Lunch, Snacks, Dinner
    public float quantity;
    /** "count" or "g" */
    public String quantityUnit;
    public float calories;
    public float protein;
    public float carbs;
    public float fats;
    public float fiber;
    public int sortOrder;
}
