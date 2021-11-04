package com.conboi.plannerapp.ui.main

import androidx.lifecycle.*
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.SortOrder
import com.conboi.plannerapp.data.SynchronizationState
import com.conboi.plannerapp.data.TaskDao
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.myclass.FirebaseUserLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


const val MAX_TASK_COUNT: Int = 50
const val MAX_ADD_TASK: Int = 15

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    savedState: SavedStateHandle
) : ViewModel() {
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

    fun onTaskCheckedChanged(task: TaskType, checked: Boolean, increase: Boolean) =
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
                } else {
                    taskDao.update(
                        task.copy(
                            completed = System.currentTimeMillis(),
                            checked = true,
                            totalChecked = task.totalChecked + 1
                        )
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
                        preferencesManager.decrementTotalCompleted()
                    } else {
                        taskDao.update(
                            task.copy(
                                completed = System.currentTimeMillis(),
                                checked = checked,
                                totalChecked = 1
                            )
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
                    preferencesManager.decrementTotalCompleted()
                }
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
        deadline: Long,
        priority: Int,
        check: Boolean,
        completed: Long,
        repeatMode: Int,
        totalChecked: Int
    ) =
        viewModelScope.launch {
            taskDao.update(
                task.copy(
                    title = title,
                    description = description,
                    time = time,
                    deadline = deadline,
                    priority = priority,
                    checked = check,
                    completed = completed,
                    repeatMode = repeatMode,
                    totalChecked = totalChecked
                )
            )
        }


    //Delete tasks
    fun swipeDeleteTask(task: TaskType) =
        viewModelScope.launch {
            taskDao.delete(task)
        }

    fun onUndoDeleteClick(task: TaskType) =
        viewModelScope.launch {
            taskDao.insert(task)
        }

    fun deleteOnlyCompletedTasks() =
        viewModelScope.launch {
            taskDao.deleteOnlyCompletedTasks()
        }

    fun deleteOnlyOvercompletedTasks() =
        viewModelScope.launch {
            taskDao.deleteOnlyOvercompletedTasks()
        }


    fun deleteAllTasks() = viewModelScope.launch {
        taskDao.deleteAllTasks()
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


}