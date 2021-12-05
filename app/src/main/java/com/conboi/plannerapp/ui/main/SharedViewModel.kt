package com.conboi.plannerapp.ui.main

import android.content.Context
import androidx.annotation.ColorRes
import androidx.lifecycle.*
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.SortOrder
import com.conboi.plannerapp.data.SynchronizationState
import com.conboi.plannerapp.data.TaskDao
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.MAX_ADD_TASK
import com.conboi.plannerapp.utils.MAX_TASK_COUNT
import com.conboi.plannerapp.utils.myclass.AlarmMethods
import com.conboi.plannerapp.utils.myclass.FirebaseUserLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    savedState: SavedStateHandle,
    private val alarmMethods: AlarmMethods
) : ViewModel() {
    private val _colorPrimary = MutableLiveData<Int>()
    val colorPrimary: LiveData<Int> = _colorPrimary

    private val _colorPrimaryVariant = MutableLiveData<Int>()
    val colorPrimaryVariant: LiveData<Int> = _colorPrimaryVariant

    private val _colorSecondary = MutableLiveData<Int>()
    val colorSecondary: LiveData<Int> = _colorSecondary

    private val _colorSecondaryVariant = MutableLiveData<Int>()
    val colorSecondaryVariant: LiveData<Int> = _colorSecondaryVariant

    fun updateColorPalette(
        @ColorRes p: Int,
        @ColorRes pL: Int,
        @ColorRes s: Int,
        @ColorRes sD: Int
    ) {
        _colorPrimary.value = p
        _colorPrimaryVariant.value = pL
        _colorSecondary.value = s
        _colorSecondaryVariant.value = sD
    }


    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            FirebaseUserLiveData.AuthenticationState.AUTHENTICATED
        } else {
            FirebaseUserLiveData.AuthenticationState.UNAUTHENTICATED
        }
    }

    val preferencesFlow = preferencesManager.preferencesFlow

    //Sizes
    val maxTasksCount: Int = MAX_TASK_COUNT
    val maxAddTask: Int = MAX_ADD_TASK
    val allTasksSize: LiveData<Int> = taskDao.getTasksSize()
    val allOnlyCompletedTasksSize: LiveData<Int> = taskDao.getTasksOnlyCompletedCount()
    val allOnlyOverCompletedTasksSize: LiveData<Int> = taskDao.getTasksOvercompletedCount()

    //Settings
    val appLanguage: LiveData<String> =
        combine(preferencesFlow) { it.first().languageState }.asLiveData()

    val privateModeState: LiveData<Boolean> =
        combine(preferencesFlow) { it.first().privateModeState }.asLiveData()

    val vibrationModeState: LiveData<Boolean> =
        combine(preferencesFlow) { it.first().vibrationModeState }.asLiveData()

    val remindersModeState: LiveData<Boolean> =
        combine(preferencesFlow) { it.first().remindersModeState }.asLiveData()

    val notificationsModeState: LiveData<Boolean> =
        combine(preferencesFlow) { it.first().notificationsModeState }.asLiveData()

    val totalCompleted =
        combine(preferencesManager.preferencesFlow) { it.first().totalCompleted }.asLiveData()

    val syncState = combine(preferencesFlow) {
        it.first().syncState
    }.asLiveData()

    val searchQuery = savedState.getLiveData("searchQuery", "")

    @ExperimentalCoroutinesApi
    private val tasksFlow = combine(
        searchQuery.asFlow(),
        preferencesFlow
    )
    { searchQuery,
      filterPreferences ->
        Pair(
            searchQuery,
            filterPreferences
        )
    }.flatMapLatest { (searchQuery, filterPreferences) ->
        taskDao.getSortedTasks(
            searchQuery,
            filterPreferences.sortOrder,
            filterPreferences.hideCompleted,
            filterPreferences.hideOvercompleted
        )
    }

    @ExperimentalCoroutinesApi
    val sortedTasks: LiveData<List<TaskType>> = tasksFlow.asLiveData()
    val allTasks: LiveData<List<TaskType>> = taskDao.getAllTasks().asLiveData()

    fun onTitleChanged(task: TaskType, title: String) = viewModelScope.launch {
        taskDao.update(task.copy(title = title))
    }

    fun onTaskCheckedChanged(
        task: TaskType,
        checked: Boolean,
        increase: Boolean,
        context: Context
    ) =
        viewModelScope.launch {
            if (increase) {
                if (task.totalChecked == 0) {
                    taskDao.update(
                        task.copy(
                            completed = System.currentTimeMillis(),
                            checked = true,
                            totalChecked = 1
                        )
                    )
                    utilsSetNotification(
                        context,
                        task.idTask,
                        task.repeatMode,
                        task.time,
                        task.deadline
                    )
                } else {
                    taskDao.update(
                        task.copy(
                            completed = System.currentTimeMillis(),
                            checked = true,
                            totalChecked = task.totalChecked + 1
                        )
                    )
                    utilsSetNotification(
                        context,
                        task.idTask,
                        task.repeatMode,
                        task.time,
                        task.deadline,
                    )
                }
                preferencesManager.incrementTotalCompleted()
            } else {
                if (checked) {
                    if (task.totalChecked > 1) {
                        taskDao.update(
                            task.copy(
                                totalChecked = task.totalChecked - 1,
                                completed = System.currentTimeMillis(),
                            )
                        )
                        utilsSetNotification(
                            context,
                            task.idTask,
                            task.repeatMode,
                            task.time,
                            task.deadline,
                        )
                        preferencesManager.decrementTotalCompleted()
                    } else {
                        taskDao.update(
                            task.copy(
                                completed = System.currentTimeMillis(),
                                checked = checked,
                                totalChecked = 1
                            )
                        )
                        utilsSetNotification(
                            context,
                            task.idTask,
                            task.repeatMode,
                            task.time,
                            task.deadline
                        )
                        preferencesManager.incrementTotalCompleted()
                    }
                } else {
                    taskDao.update(
                        task.copy(
                            completed = GLOBAL_START_DATE,
                            checked = checked,
                            totalChecked = 0
                        )
                    )
                    utilsSetNotification(
                        context,
                        task.idTask,
                        task.repeatMode,
                        task.time,
                        task.deadline
                    )
                    preferencesManager.decrementTotalCompleted()
                }
            }
        }

    private fun utilsSetNotification(
        context: Context,
        idTask: Int,
        repeatMode: Int,
        newTime: Long,
        newDeadline: Long
    ) {
        if (newTime != GLOBAL_START_DATE) {
            alarmMethods.setReminder(
                context,
                idTask,
                repeatMode,
                newTime
            )
        }
        if (newDeadline != GLOBAL_START_DATE) {
            alarmMethods.setDeadline(context, idTask, newDeadline)
        }
    }

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

    fun onHideOvercompletedClick(hideOvercompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateHideOvercompleted(hideOvercompleted)
    }


    fun isListNotFull(): Boolean = allTasks.value!!.size < MAX_TASK_COUNT

    suspend fun getRemindersState(): Boolean = preferencesFlow.first().remindersModeState

    fun getTask(id: Int): LiveData<TaskType> = taskDao.getTask(id).asLiveData()

    suspend fun getTotalCompleted(): Int =
        preferencesManager.preferencesFlow.first().totalCompleted

    fun updateTotalCompleted(count: Int) = viewModelScope.launch {
        preferencesManager.updateTotalCompleted(count)
    }

    fun incrementTotalCompleted(differ: Int) = viewModelScope.launch {
        preferencesManager.incrementTotalCompleted(differ)
    }

    fun decrementTotalCompleted(differ: Int) = viewModelScope.launch {
        preferencesManager.decrementTotalCompleted(differ)
    }

    //Insert tasks
    fun insertAllTasks(taskList: List<TaskType>) =
        viewModelScope.launch {
            taskDao.insertAll(taskList)
        }

    fun insertTask(task: TaskType) =
        viewModelScope.launch {
            taskDao.insert(task)
        }

    fun insertAllTasks(task: TaskType, enteredAmount: Int) =
        viewModelScope.launch {
            repeat(enteredAmount) {
                taskDao.insert(task)
            }
        }


    //Update tasks
    fun updateTask(
        task: TaskType,
        title: String,
        description: String,
        time: Long,
        repeatMode: Int,
        deadline: Long,
        priority: Int,
        checked: Boolean,
        totalChecked: Int,
        completed: Long,
        missed: Boolean
    ) =
        viewModelScope.launch {
            taskDao.update(
                task.copy(
                    title = title,
                    description = description,
                    time = time,
                    repeatMode = repeatMode,
                    deadline = deadline,
                    priority = priority,
                    checked = checked,
                    totalChecked = totalChecked,
                    completed = completed,
                    missed = missed
                )
            )
        }


    //Delete tasks
    fun swipeDeleteTask(task: TaskType) =
        viewModelScope.launch {
            taskDao.delete(task)
        }

    fun onUndoDeleteClick(task: TaskType, context: Context) =
        viewModelScope.launch {
            if (task.time != GLOBAL_START_DATE) {
                if (task.time <= System.currentTimeMillis() && task.repeatMode == 0) {
                    task.time = GLOBAL_START_DATE
                } else {
                    alarmMethods.setReminder(
                        context,
                        task.idTask,
                        task.repeatMode,
                        task.time
                    )
                }
            }

            if (task.deadline != GLOBAL_START_DATE) {
                if (task.deadline <= System.currentTimeMillis()) {
                    task.missed = true
                } else {
                    alarmMethods.setDeadline(
                        context,
                        task.idTask,
                        task.deadline
                    )
                }
            }
            taskDao.insert(task)
        }

    fun deleteOnlyCompletedTasks(context: Context) =
        viewModelScope.launch {
            val listOnlyCompleted = taskDao.getTasksOnlyCompleted().first().toList()
            for (task in listOnlyCompleted) {
                alarmMethods.cancelReminder(context, task.idTask)
                alarmMethods.cancelDeadline(context, task.idTask)
            }
            taskDao.deleteOnlyCompletedTasks()
        }

    fun deleteOnlyOvercompletedTasks(context: Context) =
        viewModelScope.launch {
            val listOnlyCompleted = taskDao.getTasksOvercompleted().first().toList()
            for (task in listOnlyCompleted) {
                 alarmMethods.cancelReminder(context, task.idTask)
                 alarmMethods.cancelDeadline(context, task.idTask)
            }
            taskDao.deleteOnlyOvercompletedTasks()
        }


    fun deleteAllTasks(context: Context) = viewModelScope.launch {
        taskDao.deleteAllTasks()
        alarmMethods.cancelAllAlarmsType(context, null)
    }


    fun updatePrivateModeState(privateState: Boolean) = viewModelScope.launch {
        preferencesManager.updatePrivateModeState(privateState)
    }

    fun updateVibrationModeState(vibrationState: Boolean) = viewModelScope.launch {
        preferencesManager.updateVibrationModeState(vibrationState)
    }

    fun updateRemindersModeState(remindersState: Boolean) = viewModelScope.launch {
        preferencesManager.updateRemindersModeState(remindersState)
    }

    fun updateNotificationsModeState(notificationsState: Boolean) = viewModelScope.launch {
        preferencesManager.updateNotificationsModeState(notificationsState)
    }

    fun updateSyncState(sync: SynchronizationState) =
        viewModelScope.launch { preferencesManager.updateSyncState(sync) }

    fun updateLanguageState(locale: String) =
        viewModelScope.launch { preferencesManager.updateLanguageState(locale) }

    fun signOutDelete() =
        viewModelScope.launch {
            taskDao.deleteAllTasks()
            preferencesManager.updateTotalCompleted(0)
        }
}