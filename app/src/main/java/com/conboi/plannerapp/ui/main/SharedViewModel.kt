package com.conboi.plannerapp.ui.main

import android.content.Context
import androidx.annotation.ColorRes
import androidx.lifecycle.*
import com.conboi.plannerapp.data.*
import com.conboi.plannerapp.di.IoDispatcher
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.MAX_ADD_TASK
import com.conboi.plannerapp.utils.MAX_TASK_COUNT
import com.conboi.plannerapp.utils.MIDDLE_COUNT
import com.conboi.plannerapp.utils.myclass.AlarmMethods
import com.conboi.plannerapp.utils.myclass.FirebaseUserLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    savedState: SavedStateHandle,
    private val alarmMethods: AlarmMethods,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher
) : ViewModel() {
    private val _colorPrimaryVariant = MutableLiveData<Int>()
    val colorPrimaryVariant: LiveData<Int> = _colorPrimaryVariant

    fun updateColor(
        @ColorRes pL: Int
    ) {
        _colorPrimaryVariant.value = pL
    }


    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            FirebaseUserLiveData.AuthenticationState.AUTHENTICATED
        } else {
            FirebaseUserLiveData.AuthenticationState.UNAUTHENTICATED
        }
    }

    val selectedPremiumType = MutableLiveData<Int>()

    val preferencesFlow = preferencesManager.preferencesFlow

    //Settings
    val premiumState: LiveData<Boolean> =
        combine(preferencesFlow) { it.first().premiumState }.asLiveData()

    val premiumType =
        combine(preferencesManager.preferencesFlow) { it.first().premiumType }.asLiveData()

    val middleListTime: LiveData<Long> =
        combine(preferencesFlow) { it.first().middleListTime }.asLiveData()

    val privateModeState: LiveData<Boolean> =
        combine(preferencesFlow) { it.first().privateModeState }.asLiveData()

    val vibrationModeState: LiveData<Boolean> =
        combine(preferencesFlow) { it.first().vibrationModeState }.asLiveData()

    val remindersModeState: LiveData<Boolean> =
        combine(preferencesFlow) { it.first().remindersModeState }.asLiveData()

    val notificationsModeState: LiveData<Boolean> =
        combine(preferencesFlow) { it.first().notificationsModeState }.asLiveData()

    val totalCompleted =
        combine(preferencesFlow) { it.first().totalCompleted }.asLiveData()

    val syncState = combine(preferencesFlow) {
        it.first().syncState
    }.asLiveData()

    val rateUsCount: LiveData<Int> =
        combine(preferencesFlow) { it.first().rateUsCount }.asLiveData()


    //Sizes
    val maxTasksCount: Int = MAX_TASK_COUNT
    val maxAddTask: Int = MAX_ADD_TASK
    val allTasksSize: LiveData<Int> = taskDao.getTasksSize()
    val allOnlyCompletedTasksSize: LiveData<Int> = taskDao.getTasksOnlyCompletedCount()
    val allOnlyOverCompletedTasksSize: LiveData<Int> = taskDao.getTasksOvercompletedCount()

    val searchQuery = savedState.getLiveData("searchQuery", "")

    @ExperimentalCoroutinesApi
    private val tasksFlow =
        combine(
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

    fun onTitleChanged(task: TaskType, title: String) = viewModelScope.launch{
        withContext(dispatcherIO) {
            taskDao.update(task.copy(title = title))
        }
    }

    fun onTaskCheckedChanged(
        task: TaskType,
        checked: Boolean,
        increase: Boolean
    ) =
        viewModelScope.launch {
            withContext(dispatcherIO) {
                if (increase) {
                    if (task.totalChecked == 0) {
                        taskDao.update(
                            task.copy(
                                completed = System.currentTimeMillis(),
                                lastOvercheck = System.currentTimeMillis(),
                                checked = true,
                                totalChecked = 1
                            )
                        )
                    } else {
                        taskDao.update(
                            task.copy(
                                completed = System.currentTimeMillis(),
                                lastOvercheck = System.currentTimeMillis(),
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
                                    lastOvercheck = GLOBAL_START_DATE,
                                    totalChecked = task.totalChecked - 1
                                )
                            )
                            preferencesManager.decrementTotalCompleted()
                        } else {
                            taskDao.update(
                                task.copy(
                                    completed = System.currentTimeMillis(),
                                    lastOvercheck = System.currentTimeMillis(),
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
                                lastOvercheck = GLOBAL_START_DATE,
                                checked = checked,
                                totalChecked = 0
                            )
                        )
                        preferencesManager.decrementTotalCompleted()
                    }
                }
            }
        }


    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        withContext(dispatcherIO) {
            preferencesManager.updateSortOrder(sortOrder)
        }
    }

    fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
        withContext(dispatcherIO) {
            preferencesManager.updateHideCompleted(hideCompleted)
        }
    }

    fun onHideOvercompletedClick(hideOvercompleted: Boolean) = viewModelScope.launch {
        withContext(dispatcherIO) {
            preferencesManager.updateHideOvercompleted(hideOvercompleted)
        }
    }


    fun isListNotFull(): Boolean =
        allTasks.value!!.size < MAX_TASK_COUNT + if (premiumState.value == true) MIDDLE_COUNT else 0

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
            withContext(dispatcherIO) {
                taskDao.insertAll(taskList)
            }
        }

    fun insertAllTasks(task: TaskType, enteredAmount: Int) =
        viewModelScope.launch (dispatcherIO){
            repeat(enteredAmount) {
                if (rateUsCount.value!! <= 15) {
                    updateRateUs(null)
                }
                taskDao.insert(task)
                isMiddleList(System.currentTimeMillis(), false)
            }
        }


    fun insertTask(task: TaskType) =
        viewModelScope.launch (dispatcherIO){
            if (rateUsCount.value!! <= 15) {
                updateRateUs(null)
            }
            taskDao.insert(task)
            isMiddleList(System.currentTimeMillis(), false)
        }


    fun isMiddleList(downloadMiddle: Long, force: Boolean) {
        if (allTasksSize.value!! == MIDDLE_COUNT || force) {
            viewModelScope.launch {
                preferencesManager.updateMiddleListTime(downloadMiddle)
            }
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
        missed: Boolean,
        lastOvercheck: Long
    ) =
        viewModelScope.launch {
            withContext(dispatcherIO) {
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
                        missed = missed,
                        lastOvercheck = lastOvercheck
                    )
                )
            }
        }


    //Delete tasks
    fun swipeDeleteTask(task: TaskType) =
        viewModelScope.launch {
            taskDao.delete(task)
        }

    fun onUndoDeleteClick(task: TaskType, context: Context) =
        viewModelScope.launch (dispatcherIO){
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
        viewModelScope.launch (dispatcherIO){
            val listOnlyCompleted = taskDao.getTasksOnlyCompleted().first().toList()
            for (task in listOnlyCompleted) {
                alarmMethods.cancelReminder(context, task.idTask)
                alarmMethods.cancelDeadline(context, task.idTask)
            }
            taskDao.deleteOnlyCompletedTasks()
        }

    fun deleteOnlyOvercompletedTasks(context: Context) =
        viewModelScope.launch {
            withContext(dispatcherIO) {
                val listOnlyCompleted = taskDao.getTasksOvercompleted().first().toList()
                for (task in listOnlyCompleted) {
                    alarmMethods.cancelReminder(context, task.idTask)
                    alarmMethods.cancelDeadline(context, task.idTask)
                }
                taskDao.deleteOnlyOvercompletedTasks()
            }
        }


    fun deleteAllTasks(context: Context) = viewModelScope.launch {
        withContext(dispatcherIO) {
            taskDao.deleteAllTasks()
            alarmMethods.cancelAllAlarmsType(context, null)
        }
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

    fun updatePremium(state: Boolean) =
        viewModelScope.launch {
            preferencesManager.updatePremium(
                state
            )
        }

    fun updatePremiumType(type: PremiumType) = viewModelScope.launch {
        preferencesManager.updatePremiumType(type)
    }

    fun updateRateUs(count: Int?) = viewModelScope.launch { preferencesManager.updateRateUs(count) }

    fun signOutDelete() =
        viewModelScope.launch {
            withContext(dispatcherIO){
                taskDao.deleteAllTasks()
                preferencesManager.signOut()
            }

        }


}