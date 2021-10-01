package com.conboi.plannerapp.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.conboi.plannerapp.data.model.TaskType

class TaskDetailsViewModel : ViewModel() {
    private var _bufferTask = MutableLiveData(TaskType(title = null, description = null))
    val bufferTask: LiveData<TaskType> = _bufferTask

    private var _newTaskTime = MutableLiveData(0)
    val newTaskTime: LiveData<Int> = _newTaskTime


    private var _newTaskPriority = MutableLiveData(1)
    val newTaskPriority: LiveData<Int> = _newTaskPriority

    private var _newTaskCompleted = MutableLiveData<Long>(0)
    val newTaskCompleted: LiveData<Long> = _newTaskCompleted

    private var _newTaskChecked = MutableLiveData(false)
    val newTaskChecked: LiveData<Boolean> = _newTaskChecked


    fun setBufferTask(task: TaskType) {
        _bufferTask.value = task
    }

    fun updateTimeValue(time: Int) {
        _newTaskTime.value = time
    }

    fun updatePriorityValue(priority: Int) {
        _newTaskPriority.value = priority
    }

    fun updateCompletedValue(completed: Long, checked: Boolean) {
        if (checked) {
            _newTaskCompleted.value = completed
        } else {
            _newTaskCompleted.value = 0
        }
    }

    fun updateCheckedValue(checked: Boolean) {
        updateCompletedValue(System.currentTimeMillis(), checked)
        _newTaskChecked.value = checked
    }
}