package com.conboi.plannerapp.data.source.local.repo

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import com.conboi.plannerapp.data.dao.TaskDao
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.data.source.local.preferences.UserPreferencesDataStore
import com.conboi.plannerapp.di.IODispatcher
import com.conboi.plannerapp.interfaces.UpdateTotalTaskCallback
import com.conboi.plannerapp.utils.AlarmType
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.RepeatMode
import com.conboi.plannerapp.utils.SortOrder
import com.conboi.plannerapp.utils.shared.AlarmUtil
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject


@Module
@InstallIn(ActivityRetainedComponent::class)
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmUtil: AlarmUtil,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val userPreferencesFlow = userPreferencesDataStore.preferencesFlow

    fun getTask(id: Int) = taskDao.getTask(id).asLiveData()

    fun getTaskSize() = taskDao.getTaskSize()
    fun getCompletedTaskCount() = taskDao.getCompletedTaskSize()
    fun getOvercompletedTaskCount() = taskDao.getOvercompletedTaskSize()

    fun getSortedTasks(searchQuery: LiveData<String> = MutableLiveData("")): LiveData<List<TaskType>> {
        return combine(searchQuery.asFlow(), userPreferencesFlow) { searchString, filterPref ->
            Pair(
                searchString,
                filterPref
            )
        }.flatMapLatest { (searchValue, preferences) ->
            taskDao.getSortedTasks(
                searchValue,
                preferences.sortOrder,
                preferences.isHideCompleted,
                preferences.isHideOvercompleted
            )
        }.flowOn(ioDispatcher).asLiveData()
    }

    // Insert actions
    suspend fun insertTask() = taskDao.insert(TaskType())

    suspend fun insertDownloadedTasks(downloadTasks: MutableList<TaskType>) = taskDao.insertAll(downloadTasks)


    // Update actions
    suspend fun onTitleChanged(task: TaskType, title: String) = taskDao.update(task.copy(title = title))

    suspend fun onTaskCheckedChanged(task: TaskType, checked: Boolean, increase: Boolean) {
        val totalValue = task.totalChecked
        if (increase) {
            // Increment total
            taskDao.update(
                task.copy(
                    completed = System.currentTimeMillis(),
                    lastOvercheck = System.currentTimeMillis(),
                    checked = true,
                    totalChecked = 1 + if (totalValue != 0) totalValue else 0
                )
            )
            userPreferencesDataStore.incrementTotalCompleted()
        } else {
            if (checked) {
                // isChecked = true
                if (totalValue > 1) {
                    // Decrement total
                    taskDao.update(
                        task.copy(
                            lastOvercheck = GLOBAL_START_DATE,
                            totalChecked = totalValue - 1
                        )
                    )
                    userPreferencesDataStore.decrementTotalCompleted()
                } else {
                    // Increment total
                    taskDao.update(
                        task.copy(
                            completed = System.currentTimeMillis(),
                            lastOvercheck = System.currentTimeMillis(),
                            checked = checked,
                            totalChecked = 1
                        )
                    )
                    userPreferencesDataStore.incrementTotalCompleted()
                }
            } else {
                // isChecked = false
                taskDao.update(
                    task.copy(
                        completed = GLOBAL_START_DATE,
                        lastOvercheck = GLOBAL_START_DATE,
                        checked = checked,
                        totalChecked = 0
                    )
                )
                userPreferencesDataStore.decrementTotalCompleted()
            }
        }
    }

    suspend fun onUndoDeleteClick(context: Context, task: TaskType) {
        val id = task.idTask
        val time = task.time
        val deadline = task.deadline
        val repeatMode = task.repeatMode

        if (time != GLOBAL_START_DATE) {
            if (time <= System.currentTimeMillis() && repeatMode == RepeatMode.Once) {
                task.time = GLOBAL_START_DATE
            } else {
                alarmUtil.setReminder(
                    context,
                    id,
                    repeatMode,
                    time
                )
            }
        }

        if (deadline != GLOBAL_START_DATE) {
            if (deadline <= System.currentTimeMillis()) {
                task.missed = true
            } else {
                alarmUtil.setDeadline(
                    context,
                    id,
                    deadline
                )
            }
        }
        taskDao.insert(task)
    }

    suspend fun onSortOrderSelected(sortOrder: SortOrder) =
        userPreferencesDataStore.updateSortOrder(sortOrder)

    suspend fun onHideCompletedClick(hideCompleted: Boolean) =
        userPreferencesDataStore.updateHideCompleted(hideCompleted)

    suspend fun onHideOvercompletedClick(hideOvercompleted: Boolean) =
        userPreferencesDataStore.updateHideOvercompleted(hideOvercompleted)

    suspend fun updateTask(newTask: TaskType) = taskDao.update(newTask)

    fun updateNewTotal(
        context: Context,
        initTask: TaskType,
        newTotal: Int,
        newTime: Long,
        newRepeatMode: RepeatMode,
        newDeadline: Long,
        callbackTotalTask: UpdateTotalTaskCallback
    ) {
        val initTotal = initTask.totalChecked
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

    private fun pendingUpdateAlarm(
        context: Context,
        initTask: TaskType,
        newTime: Long,
        newRepeatMode: RepeatMode,
        newDeadline: Long
    ) {
        val id = initTask.idTask
        val initTime = initTask.time
        val initDeadline = initTask.deadline

        if (newTime != initTime) {
            if (newTime != GLOBAL_START_DATE) {
                alarmUtil.setReminder(
                    context,
                    id,
                    newRepeatMode,
                    newTime
                )
            } else {
                alarmUtil.cancelReminder(context, id)
            }
        }

        if (newDeadline != initDeadline) {
            if (newDeadline != GLOBAL_START_DATE) {
                alarmUtil.setDeadline(
                    context,
                    id,
                    newDeadline
                )
            } else {
                alarmUtil.cancelDeadline(context, id)
            }
        }
    }


    // Delete actions
    suspend fun swipeDeleteTask(context: Context, task: TaskType) {
        alarmUtil.cancelReminder(context, task.idTask)
        alarmUtil.cancelDeadline(context, task.idTask)
        taskDao.delete(task)
    }

    suspend fun deleteAllTasks(context: Context) {
        taskDao.deleteAllTasks()
        alarmUtil.cancelAllAlarmsType(context, AlarmType.ALL)
    }

    suspend fun deleteCompletedTasks(context: Context) {
        val listOnlyCompleted = taskDao.getCompletedTasks().first()
        for (task in listOnlyCompleted) {
            alarmUtil.cancelReminder(context, task.idTask)
            alarmUtil.cancelDeadline(context, task.idTask)
        }
        taskDao.deleteCompletedTasks()
    }

    suspend fun deleteOvercompletedTasks(context: Context) {
        val listOnlyOvercompleted = taskDao.getOvercompletedTasks().first().toList()
        for (task in listOnlyOvercompleted) {
            alarmUtil.cancelReminder(context, task.idTask)
            alarmUtil.cancelDeadline(context, task.idTask)
        }
        taskDao.deleteOvercompletedTasks()
    }

    fun cancelAllAlarmsType(context: Context, alarmType: AlarmType) =
        alarmUtil.cancelAllAlarmsType(context, alarmType)
}