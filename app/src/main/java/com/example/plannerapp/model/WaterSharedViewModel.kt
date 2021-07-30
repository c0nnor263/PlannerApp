package com.example.plannerapp.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.PreferencesManager
import com.example.plannerapp.data.SortOrder
import com.example.plannerapp.data.TaskType
import com.example.plannerapp.data.TaskTypeDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


const val MAX_TASK_COUNT: Int = 25

@HiltViewModel
class WaterSharedViewModel @Inject constructor(
    private val taskTypeDao: TaskTypeDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val maxTasksCount: Int get() = MAX_TASK_COUNT
    val allTasksSize: LiveData<Int> = taskTypeDao.getTasksSize()

    val searchQuery = MutableStateFlow("")
    val preferencesFlow = preferencesManager.preferencesFlow


    private val tasksFlow = combine(
        searchQuery,
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

    fun onTaskSelected(task: TaskType) {

    }
    fun onTaskCheckedChanged(task: TaskType, checked: Boolean) = viewModelScope.launch {
        taskTypeDao.update(task.copy(checkTask = checked))
    }
    fun onNameChanged(task: TaskType, name:String) = viewModelScope.launch {
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
    fun addNewTask(
        taskName: String?,
        taskDescription: String?,
        taskTime: Int,
        taskPriority: Int,
        taskChecked: Boolean
    ) {
        val newTask = getNewTaskEntry(
            taskName,
            taskDescription,
            taskTime,
            taskPriority,
            taskChecked
        )
        insertTask(newTask)
    }
    fun addNewTasks(
        taskName: String?,
        taskDescription: String?,
        taskTime: Int,
        taskPriority: Int,
        taskChecked: Boolean,
        enteredAmount: Int
    ) {
        val newTask = getNewTaskEntry(
            taskName,
            taskDescription,
            taskTime,
            taskPriority,
            taskChecked
        )
        for (i in 1..enteredAmount) {
            insertTask(newTask)
        }
    }
    fun updateTask(
        taskId: Int,
        taskName: String?,
        taskDescription: String?,
        taskTime: Int,
        taskPriority: Int,
        taskChecked: Boolean
    ) {
        val updatedTask = getUpdatedTaskEntry(
            taskId,
            taskName,
            taskDescription,
            taskTime,
            taskPriority,
            taskChecked
        )
        updateTask(updatedTask)
    }


    fun isListNotFull(): Boolean {
        return (allTasks.value!!.size < MAX_TASK_COUNT)
    }

    private fun insertTask(taskType: TaskType) {
        viewModelScope.launch {
            taskTypeDao.insert(taskType)
        }
    }

    private fun updateTask(taskType: TaskType) {
        viewModelScope.launch {
            taskTypeDao.update(taskType)
        }
    }

    fun swipeDeleteTask(task: TaskType) =
        viewModelScope.launch {
            taskTypeDao.delete(task)
            tasksEventChannel.send(TasksEvent.ShowUndoDeleteTaskMessage(task))
        }

    fun onUndoDeleteClick(task: TaskType) = viewModelScope.launch {
        taskTypeDao.insert(task)
    }

    fun deleteCompletedTasks() =
        viewModelScope.launch {
            taskTypeDao.deleteCompletedTasks()
        }


    fun deleteAllData() =
        viewModelScope.launch {
            taskTypeDao.deleteAllData()
        }

    private fun getNewTaskEntry(
        taskName: String?,
        taskDescription: String?,
        taskTime: Int,
        taskPriority: Int,
        taskChecked: Boolean
    ): TaskType {
        return TaskType(
            nameTask = taskName,
            descriptionTask = taskDescription,
            timeTask = taskTime,
            priorityTask = taskPriority,
            checkTask = taskChecked
        )
    }

    private fun getUpdatedTaskEntry(
        taskId: Int,
        taskName: String?,
        taskDescription: String?,
        taskTime: Int,
        taskPriority: Int,
        taskChecked: Boolean
    ): TaskType {
        return TaskType(
            idTask = taskId,
            nameTask = taskName,
            descriptionTask = taskDescription,
            timeTask = taskTime,
            priorityTask = taskPriority,
            checkTask = taskChecked
        )
    }




}