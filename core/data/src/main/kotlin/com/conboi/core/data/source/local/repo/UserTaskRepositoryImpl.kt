package com.conboi.core.data.source.local.repo

import android.content.Context
import com.conboi.core.data.source.local.mapper.UserTaskMapper
import com.conboi.core.database.dao.UserTaskDao
import com.conboi.core.database.preferences.UserPreferencesDataStore
import com.conboi.core.domain.GLOBAL_START_DATE
import com.conboi.core.domain.di.IODispatcher
import com.conboi.core.domain.old.enums.RepeatMode
import com.conboi.core.domain.old.enums.SortOrder
import com.conboi.core.domain.old.interfaces.UpdateTotalTaskCallback
import com.conboi.core.domain.source.local.model.UserTaskDTO
import com.conboi.core.domain.source.local.repo.UserTaskRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class UserTaskRepositoryImpl
@Inject
constructor(
    private val taskDao: UserTaskDao,
    private val userTaskMapper: UserTaskMapper,
//        private val alarmUtil: AlarmUtil,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : UserTaskRepository {
    private val userPreferencesFlow = userPreferencesDataStore.preferencesFlow

    override fun getTask(id: Int): Flow<UserTaskDTO> {
        return taskDao.getTask(id)
            .map {
                userTaskMapper.from(it)
            }
    }

    override fun getTasksSize(): Flow<Int> = taskDao.getTaskSize()

    override fun getCompletedTasksCount(): Flow<Int> = taskDao.getCompletedTaskSize()

    override fun getMultipleCompletedTasksCount(): Flow<Int> = taskDao.getMultipleCompletedTaskSize()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSortedTasks(searchQuery: String): Flow<List<UserTaskDTO>> {
        return userPreferencesFlow.flatMapLatest { preferences ->
            val data =
                taskDao.getSortedTasks(
                    searchQuery, // Use searchQuery directly
                    preferences.sortOrder,
                    preferences.isHideCompleted,
                    preferences.isHideOvercompleted
                )
            data.map {
                it.map { task ->
                    userTaskMapper.from(task)
                }
            }
        }.flowOn(ioDispatcher)
    }

    // Insert actions
    override suspend fun insertEmptyTask() {
        val newTask = userTaskMapper.to(UserTaskDTO())
        taskDao.insert(newTask)
    }

    override suspend fun insertDownloadedTasks(downloadTasks: List<UserTaskDTO>) {
        val data =
            downloadTasks.map {
                userTaskMapper.to(it)
            }
        taskDao.insertAll(data)
    }

    // Update actions
    override suspend fun onTitleChanged(
        task: UserTaskDTO,
        title: String
    ) {
        val newTaskDTO = task.copy(title = title)
        taskDao.update(userTaskMapper.to(newTaskDTO))
    }

    override suspend fun onTaskCheckedChanged(
        task: UserTaskDTO,
        checked: Boolean,
        increase: Boolean
    ) {
        val totalValue = task.totalCompleted
        if (increase) {
            val newTaskDTO =
                task.copy(
                    completedTime = System.currentTimeMillis(),
                    lastMultipleCompleteTime = System.currentTimeMillis(),
                    isCompleted = true,
                    totalCompleted = 1 + if (totalValue != 0) totalValue else 0
                )
            // Increment total
            taskDao.update(userTaskMapper.to(newTaskDTO))
            userPreferencesDataStore.incrementTotalCompleted()
        } else {
            if (checked) {
                // isChecked = true
                if (totalValue > 1) {
                    val newTaskDTO =
                        task.copy(
                            lastMultipleCompleteTime = GLOBAL_START_DATE,
                            totalCompleted = totalValue - 1
                        )
                    // Decrement total
                    taskDao.update(userTaskMapper.to(newTaskDTO))
                    userPreferencesDataStore.decrementTotalCompleted()
                } else {
                    val newTaskDTO =
                        task.copy(
                            completedTime = System.currentTimeMillis(),
                            lastMultipleCompleteTime = System.currentTimeMillis(),
                            isCompleted = true,
                            totalCompleted = 1
                        )
                    // Increment total
                    taskDao.update(userTaskMapper.to(newTaskDTO))
                    userPreferencesDataStore.incrementTotalCompleted()
                }
            } else {
                val newTaskDTO =
                    task.copy(
                        completedTime = GLOBAL_START_DATE,
                        lastMultipleCompleteTime = GLOBAL_START_DATE,
                        isCompleted = false,
                        totalCompleted = 0
                    )
                taskDao.update(userTaskMapper.to(newTaskDTO))
                userPreferencesDataStore.decrementTotalCompleted()
            }
        }
    }

    override suspend fun onUndoDeleteClick(
        context: Context,
        task: UserTaskDTO
    ) {
        // Undo delete task
        task.id
        val time = task.reminderTime
        val deadline = task.deadlineTime
        val repeatMode = task.reminderRepeatMode

        if (time != GLOBAL_START_DATE) {
            if (time <= System.currentTimeMillis() && repeatMode == RepeatMode.Once) {
                val newTaskDTO =
                    task.copy(
                        reminderTime = GLOBAL_START_DATE
                    )
                taskDao.update(userTaskMapper.to(newTaskDTO))
            } else {
//                    alarmUtil.setReminder(
//                        context,
//                        id,
//                        repeatMode,
//                        time
//                    )
            }
        }

        if (deadline != GLOBAL_START_DATE) {
            if (deadline <= System.currentTimeMillis()) {
                val newTaskDTO =
                    task.copy(
                        isMissed = true
                    )
                taskDao.update(userTaskMapper.to(newTaskDTO))
            } else {
//                    alarmUtil.setDeadline(
//                        context,
//                        id,
//                        deadline
//                    )
            }
        }
        taskDao.insert(userTaskMapper.to(task))
    }

    override suspend fun onSortOrderSelected(sortOrder: SortOrder): Unit =
        userPreferencesDataStore.updateSortOrder(sortOrder)

    override suspend fun onHideCompletedClick(hideCompleted: Boolean): Unit =
        userPreferencesDataStore.updateHideCompleted(hideCompleted)

    override suspend fun onHideMultipleCompletedClick(hideMultipleCompleted: Boolean): Unit =
        userPreferencesDataStore.updateHideOvercompleted(
            hideMultipleCompleted
        )

    override suspend fun updateTask(newTask: UserTaskDTO) {
        taskDao.update(userTaskMapper.to(newTask))
    }

    override fun updateNewTotal(
        context: Context,
        initTask: UserTaskDTO,
        newTotal: Int,
        newTime: Long,
        newRepeatMode: RepeatMode,
        newDeadline: Long,
        callbackTotalTask: UpdateTotalTaskCallback
    ) {
        val initTotal = initTask.totalCompleted
        // Update new total checked value
        if (newTotal > initTotal) {
            val differ = newTotal - initTotal - if (initTotal == 0) 1 else 0
            callbackTotalTask.onIncrement(differ)
        } else if (newTotal < initTotal) {
            val differ = initTotal - newTotal - if (initTotal == 0) 1 else 0
            callbackTotalTask.onDecrement(differ)
        }

        // Update alarms
        pendingUpdateAlarm(context, initTask, newTime, newRepeatMode, newDeadline)
    }

    override fun pendingUpdateAlarm(
        context: Context,
        initTask: UserTaskDTO,
        newTime: Long,
        newRepeatMode: RepeatMode,
        newDeadline: Long
    ) {
        initTask.id
        val initTime = initTask.reminderTime
        val initDeadline = initTask.deadlineTime

        if (newTime != initTime) {
            if (newTime != GLOBAL_START_DATE) {
//                    alarmUtil.setReminder(
//                        context,
//                        id,
//                        newRepeatMode,
//                        newTime
//                    )
            } else {
//                    alarmUtil.cancelReminder(context, id)
            }
        }

        if (newDeadline != initDeadline) {
            if (newDeadline != GLOBAL_START_DATE) {
//                    alarmUtil.setDeadline(
//                        context,
//                        id,
//                        newDeadline
//                    )
            } else {
//                    alarmUtil.cancelDeadline(context, id)
            }
        }
    }

    // Delete actions
    override suspend fun swipeDeleteTask(
        context: Context,
        task: UserTaskDTO
    ) {
//            alarmUtil.cancelReminder(context, task.id)
//            alarmUtil.cancelDeadline(context, task.id)
        taskDao.delete(userTaskMapper.to(task))
    }

    override suspend fun deleteAllTasks(context: Context) {
        taskDao.deleteAllTasks()
//            alarmUtil.cancelAllAlarmsType(context, AlarmType.ALL)
    }

    override suspend fun deleteCompletedTasks(context: Context) {
        taskDao.getCompletedTasks().first()
//            for (task in listOnlyCompleted) {
//                alarmUtil.cancelReminder(context, task.id)
//                alarmUtil.cancelDeadline(context, task.id)
//            }
        taskDao.deleteCompletedTasks()
    }

    override suspend fun deleteMultipleCompletedTasks(context: Context) {
        taskDao.getMultipleCompletedTasks().first().toList()
//            for (task in listOnlyMultipleCompleted) {
//                alarmUtil.cancelReminder(context, task.id)
//                alarmUtil.cancelDeadline(context, task.id)
//            }
        taskDao.deleteMultipleCompletedTasks()
    }

//        //     TODO: Move to something more related to alarm
//        fun cancelAllAlarmsType(
//            context: Context,
//            alarmType: AlarmType
//        ) = alarmUtil.cancelAllAlarmsType(context, alarmType)
}