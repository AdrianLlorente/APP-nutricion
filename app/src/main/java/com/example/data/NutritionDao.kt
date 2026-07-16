package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM groups")
    fun getAllGroups(): Flow<List<Group>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group)

    @Query("SELECT * FROM nutrition_logs WHERE userId = :userId AND date = :date ORDER BY timestamp DESC")
    fun getLogsForUserAndDate(userId: String, date: String): Flow<List<NutritionLog>>

    @Query("SELECT * FROM nutrition_logs WHERE groupId = :groupId AND date = :date ORDER BY timestamp DESC")
    fun getLogsForGroupAndDate(groupId: String, date: String): Flow<List<NutritionLog>>

    @Query("SELECT * FROM nutrition_logs WHERE userId = :userId ORDER BY date ASC, timestamp DESC")
    fun getLogsForUser(userId: String): Flow<List<NutritionLog>>

    @Query("SELECT * FROM nutrition_logs WHERE groupId = :groupId ORDER BY date ASC, timestamp DESC")
    fun getLogsForGroup(groupId: String): Flow<List<NutritionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NutritionLog)

    @Delete
    suspend fun deleteLog(log: NutritionLog)

    @Query("DELETE FROM nutrition_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)
}
