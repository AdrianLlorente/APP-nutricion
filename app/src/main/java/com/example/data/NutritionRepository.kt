package com.example.data

import kotlinx.coroutines.flow.Flow

class NutritionRepository(private val dao: NutritionDao) {
    val allUsers: Flow<List<User>> = dao.getAllUsers()
    val allGroups: Flow<List<Group>> = dao.getAllGroups()

    suspend fun insertUser(user: User) {
        dao.insertUser(user)
    }

    suspend fun insertGroup(group: Group) {
        dao.insertGroup(group)
    }

    fun getLogsForUserAndDate(userId: String, date: String): Flow<List<NutritionLog>> {
        return dao.getLogsForUserAndDate(userId, date)
    }

    fun getLogsForGroupAndDate(groupId: String, date: String): Flow<List<NutritionLog>> {
        return dao.getLogsForGroupAndDate(groupId, date)
    }

    fun getLogsForUser(userId: String): Flow<List<NutritionLog>> {
        return dao.getLogsForUser(userId)
    }

    fun getLogsForGroup(groupId: String): Flow<List<NutritionLog>> {
        return dao.getLogsForGroup(groupId)
    }

    suspend fun insertLog(log: NutritionLog) {
        dao.insertLog(log)
    }

    suspend fun deleteLogById(id: Int) {
        dao.deleteLogById(id)
    }
}
