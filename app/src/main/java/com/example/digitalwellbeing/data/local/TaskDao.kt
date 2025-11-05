package com.example.digitalwellbeing.data.local

import androidx.room.*
import com.example.digitalwellbeing.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getIncompleteTasks(): Flow<List<Task>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND completedAt >= :startDate")
    fun getCompletedTasksCount(startDate: Long): Flow<Int>

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
