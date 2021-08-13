package com.conboi.plannerapp.ui.water

import android.util.Log
import androidx.lifecycle.*
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.SortOrder
import com.conboi.plannerapp.data.TaskType
import com.conboi.plannerapp.data.TaskTypeDao
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap
import kotlin.collections.component1
import kotlin.collections.set


const val MAX_TASK_COUNT: Int = 25

@ExperimentalCoroutinesApi
@HiltViewModel
class WaterSharedViewModel @Inject constructor(
    private val taskTypeDao: TaskTypeDao,
    private val preferencesManager: PreferencesManager,
    savedState: SavedStateHandle
) : ViewModel() {

    val maxTasksCount: Int get() = MAX_TASK_COUNT
    val allTasksSize: LiveData<Int> = taskTypeDao.getTasksSize()
    val allChecked: LiveData<Int> = taskTypeDao.getTasksCheckedCount()

    val searchQuery = savedState.getLiveData("searchQuery", "")
    val preferencesFlow = preferencesManager.preferencesFlow

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
        taskTypeDao.getTasks(
            searchQuery,
            filterPreferences.sortOrder,
            filterPreferences.hideCompleted
        )
    }
    val allTasks: LiveData<List<TaskType>> = tasksFlow.asLiveData()

    fun downloadTask(task:TaskType) {
        viewModelScope.launch {
            taskTypeDao.insert(task)
        }
    }

    private val tasksEventChannel = Channel<TasksEvent>()
    val taskEvent = tasksEventChannel.receiveAsFlow()

    sealed class TasksEvent {
        data class ShowUndoDeleteTaskMessage(val task: TaskType) : TasksEvent()
    }

    fun swipeDeleteTask(task: TaskType) =
        viewModelScope.launch {
            taskTypeDao.delete(task)
            tasksEventChannel.send(TasksEvent.ShowUndoDeleteTaskMessage(task))
        }

    fun onUndoDeleteClick(task: TaskType) = viewModelScope.launch {
        taskTypeDao.insert(task)
    }


    fun onTaskCheckedChanged(task: TaskType, checked: Boolean) = viewModelScope.launch {
        if (checked) {
            taskTypeDao.update(
                task.copy(
                    checkTask = checked,
                    completedTask = System.currentTimeMillis()
                )
            )
        } else {
            taskTypeDao.update(task.copy(checkTask = checked, completedTask = 0))
        }

    }

    fun onNameChanged(task: TaskType, name: String) = viewModelScope.launch {
        taskTypeDao.update(task.copy(nameTask = name))
    }


    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

    fun retrieveTask(id: Int): LiveData<TaskType> {
        return taskTypeDao.getTask(id).asLiveData()
    }


    fun isListNotFull(): Boolean {
        return (allTasks.value!!.size < MAX_TASK_COUNT)
    }

    fun insertTask(taskType: TaskType) {
        viewModelScope.launch {
            taskTypeDao.insert(taskType)
        }
    }

    fun insertTasks(taskType: TaskType, enteredAmount: Int) {
        viewModelScope.launch {
            for (i in 1..enteredAmount) {
                taskTypeDao.insert(taskType)
            }
        }
    }

    fun updateTask(
        taskType: TaskType,
        nameTask: String?,
        descriptionTask: String?,
        timeTask: Int,
        priorityTask: Int
    ) {
        viewModelScope.launch {
            taskTypeDao.update(
                taskType.copy(
                    nameTask = nameTask,
                    descriptionTask = descriptionTask,
                    timeTask = timeTask,
                    priorityTask = priorityTask
                )
            )
        }
    }


    fun deleteCompletedTasks() =
        viewModelScope.launch {
            taskTypeDao.deleteCompletedTasks()
        }


}