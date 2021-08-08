package com.example.plannerapp.ui.water

import androidx.lifecycle.*
import com.example.plannerapp.data.PreferencesManager
import com.example.plannerapp.data.SortOrder
import com.example.plannerapp.data.TaskType
import com.example.plannerapp.data.TaskTypeDao
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


const val MAX_TASK_COUNT: Int = 25

@HiltViewModel
class WaterSharedViewModel @Inject constructor(
    private val taskTypeDao: TaskTypeDao,
    private val preferencesManager: PreferencesManager,
    private val savedState: SavedStateHandle
) : ViewModel() {
    fun getCurrentUser(){
        val user = Firebase.auth.currentUser
        user?.let {
            val name = user.displayName
            val email = user.email

            // Check if user's email is verified
            val emailVerified = user.isEmailVerified

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getToken() instead.
            val uid = user.uid
        }
    }
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