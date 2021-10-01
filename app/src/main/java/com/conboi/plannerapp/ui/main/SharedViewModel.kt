package com.conboi.plannerapp.ui.main

import androidx.lifecycle.*
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.SortOrder
import com.conboi.plannerapp.data.TaskDao
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.ui.FirebaseUserLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


const val MAX_TASK_COUNT: Int = 25

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

    val maxTasksCount: Int get() = MAX_TASK_COUNT

    val allTasksSize: LiveData<Int> = taskDao.getTasksSize()
    val allCompletedTasksSize: LiveData<Int> = taskDao.getTasksCheckedCount()

    val searchQuery = savedState.getLiveData("searchQuery", "")
    val preferencesFlow = preferencesManager.preferencesFlow

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
        taskDao.getTasks(
            searchQuery,
            filterPreferences.sortOrder,
            filterPreferences.hideCompleted
        )
    }
    @ExperimentalCoroutinesApi
    val allTasks: LiveData<List<TaskType>> = tasksFlow.asLiveData()

    suspend fun getTotalCompleted(): Int = preferencesManager.preferencesFlow.first().totalCompleted
    fun downloadTask(task: TaskType) =
        viewModelScope.launch {
            taskDao.insert(task)
        }


    private val tasksEventChannel = Channel<TasksEvent>()
    val taskEvent = tasksEventChannel.receiveAsFlow()

    sealed class TasksEvent {
        data class ShowUndoDeleteTaskMessage(val task: TaskType) : TasksEvent()
    }

    fun onUndoDeleteClick(task: TaskType) = viewModelScope.launch {
        taskDao.insert(task)
    }

    fun swipeDeleteTask(task: TaskType) =
        viewModelScope.launch {
            taskDao.delete(task)
            tasksEventChannel.send(TasksEvent.ShowUndoDeleteTaskMessage(task))
        }


    fun onTaskCheckedChanged(task: TaskType, checked: Boolean) = viewModelScope.launch {
        if (checked) {
            taskDao.update(
                task.copy(
                    checked = checked,
                    completed = System.currentTimeMillis()
                )
            )
            preferencesManager.incrementTotalCompleted(preferencesFlow.first().totalCompleted)
        } else {
            taskDao.update(task.copy(checked = checked, completed = 0))
            preferencesManager.decrementTotalCompleted(preferencesFlow.first().totalCompleted)
        }

    }

    fun onTitleChanged(task: TaskType, title: String) = viewModelScope.launch {
        taskDao.update(task.copy(title = title))
    }

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

    fun onDownloadTotalCompleted(count: Int) = viewModelScope.launch {
        preferencesManager.updateTotalCompleted(count)
    }

    fun getTask(id: Int): LiveData<TaskType> = taskDao.getTask(id).asLiveData()

    @ExperimentalCoroutinesApi
    fun isListNotFull(): Boolean = allTasks.value!!.size < MAX_TASK_COUNT

    fun insertTask(task: TaskType) =
        viewModelScope.launch {
            taskDao.insert(task)
        }


    fun insertTasks(task: TaskType, enteredAmount: Int) =
        viewModelScope.launch {
            repeat(enteredAmount) {
                taskDao.insert(task)
            }
        }


    fun updateTask(
        task: TaskType,
        title: String?,
        description: String?,
        time: Int,
        priority: Int,
        check: Boolean,
        completed: Long
    ) =
        viewModelScope.launch {
            taskDao.update(
                task.copy(
                    title = title,
                    description = description,
                    time = time,
                    priority = priority,
                    checked = check,
                    completed = completed
                )
            )
        }


    fun deleteCompletedTasks() =
        viewModelScope.launch {
            taskDao.deleteCompletedTasks()
        }

}