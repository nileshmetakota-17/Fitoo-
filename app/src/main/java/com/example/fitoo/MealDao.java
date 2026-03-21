package com.example.fitoo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MealDao {

    @Insert
    long insertMeal(MealEntry meal);

    @Update
    void updateMeal(MealEntry meal);

    @Query("DELETE FROM meals WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM meals WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    List<MealEntry> getMealsBetween(long from, long to);

    @Query("SELECT * FROM meals WHERE id = :id LIMIT 1")
    MealEntry getById(int id);

    @Query("SELECT SUM(calories) FROM meals WHERE timestamp BETWEEN :from AND :to")
    Float totalCalories(long from, long to);

    @Query("SELECT SUM(protein) FROM meals WHERE timestamp BETWEEN :from AND :to")
    Float totalProtein(long from, long to);

    @Query("SELECT SUM(carbs) FROM meals WHERE timestamp BETWEEN :from AND :to")
    Float totalCarbs(long from, long to);

    @Query("SELECT SUM(fats) FROM meals WHERE timestamp BETWEEN :from AND :to")
    Float totalFats(long from, long to);

    @Query("SELECT SUM(fiber) FROM meals WHERE timestamp BETWEEN :from AND :to")
    Float totalFiber(long from, long to);

    @Query("DELETE FROM meals")
    void deleteAll();
}
