package com.focus.digitalwellbeing.data.repository

import com.focus.digitalwellbeing.data.local.TaskDao
import com.focus.digitalwellbeing.data.model.Task
import com.focus.digitalwellbeing.util.DateUtils
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getIncompleteTasks(): Flow<List<Task>> = taskDao.getIncompleteTasks()

    fun getTodayCompletedCount(): Flow<Int> =
        taskDao.getCompletedTasksCount(DateUtils.getStartOfToday())

    suspend fun addTask(title: String): Long {
        return taskDao.insertTask(Task(title = title))
    }

    suspend fun toggleTaskCompletion(task: Task) {
        val updated = task.copy(
            isCompleted = !task.isCompleted,
            completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
}

