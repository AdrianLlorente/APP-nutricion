package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val avatarEmoji: String,
    val groupId: String? = null
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey val id: String,
    val name: String
)

@Entity(tableName = "nutrition_logs")
data class NutritionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val groupId: String? = null,
    val date: String, // YYYY-MM-DD
    val foodName: String,
    val calories: Double,
    val carbs: Double,
    val proteins: Double,
    val fats: Double,
    val sugar: Double,
    val micronutrients: String = "",
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val timestamp: Long = System.currentTimeMillis()
)
