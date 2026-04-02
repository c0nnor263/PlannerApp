package com.conboi.core.domain.source.local.repo

import android.content.Context
import com.conboi.core.domain.old.enums.RepeatMode
import com.conboi.core.domain.old.enums.SortOrder
import com.conboi.core.domain.old.interfaces.UpdateTotalTaskCallback
import com.conboi.core.domain.source.local.model.UserTaskDTO
import kotlinx.coroutines.flow.Flow

interface UserTaskRepository {
    fun getTask(id: Int): Flow<UserTaskDTO>

    fun getTasksSize(): Flow<Int>

    fun getCompletedTasksCount(): Flow<Int>

    fun getMultipleCompletedTasksCount(): Flow<Int>

    fun getSortedTasks(searchQuery: String): Flow<List<UserTaskDTO>>

    // Insert actions
    suspend fun insertEmptyTask()

    suspend fun insertDownloadedTasks(downloadTasks: List<UserTaskDTO>)

    // Update actions
    suspend fun onTitleChanged(
        task: UserTaskDTO,
        title: String
    )

    suspend fun onTaskCheckedChanged(
        task: UserTaskDTO,
        checked: Boolean,
        increase: Boolean
    )

    suspend fun onUndoDeleteClick(
        context: Context,
        task: UserTaskDTO
    )

    suspend fun onSortOrderSelected(sortOrder: SortOrder)

    suspend fun onHideCompletedClick(hideCompleted: Boolean)

    suspend fun onHideMultipleCompletedClick(hideMultipleCompleted: Boolean)

    suspend fun updateTask(newTask: UserTaskDTO)

    fun updateNewTotal(
        context: Context,
        initTask: UserTaskDTO,
        newTotal: Int,
        newTime: Long,
        newRepeatMode: RepeatMode,
        newDeadline: Long,
        callbackTotalTask: UpdateTotalTaskCallback
    )

    fun pendingUpdateAlarm(
        context: Context,
        initTask: UserTaskDTO,
        newTime: Long,
        newRepeatMode: RepeatMode,
        newDeadline: Long
    )

    // Delete actions
    suspend fun swipeDeleteTask(
        context: Context,
        task: UserTaskDTO
    )

    suspend fun deleteAllTasks(context: Context)

    suspend fun deleteCompletedTasks(context: Context)

    suspend fun deleteMultipleCompletedTasks(context: Context)
}