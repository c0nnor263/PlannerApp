package com.conboi.plannerapp.ui.main

import androidx.lifecycle.*
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private var _bufferTask = MutableLiveData(TaskType())
    val bufferTask: LiveData<TaskType> = _bufferTask

    private var _newTime = MutableLiveData<Long>()
    val newTime: LiveData<Long> = _newTime

    private var _newDeadline = MutableLiveData<Long>()
    val newDeadline: LiveData<Long> = _newDeadline

    private var _newPriority = MutableLiveData<Int>()
    val newPriority: LiveData<Int> = _newPriority

    private var _newCompleted = MutableLiveData<Long>()
    val newCompleted: LiveData<Long> = _newCompleted

    private var _newChecked = MutableLiveData<Boolean>()
    val newChecked: LiveData<Boolean> = _newChecked

    private var _newRepeatMode = MutableLiveData<Int>()
    val newRepeatMode: LiveData<Int> = _newRepeatMode

    private var _newTotalChecked = MutableLiveData<Int>()
    val newTotalChecked: LiveData<Int> = _newTotalChecked

    private var _newMissed = MutableLiveData<Boolean>()
    val newMissed: LiveData<Boolean> = _newMissed

    private var _newLastOvercheck = MutableLiveData<Long>()
    val newLastOvercheck: LiveData<Long> = _newLastOvercheck

    fun setBufferTask(task: TaskType) {
        _bufferTask.value = task
    }

    fun updateTimeValue(time: Long) {
        _newTime.value = time
    }

    fun updateDeadlineValue(time: Long) {
        _newDeadline.value = time
    }

    fun updatePriorityValue(priority: Int) {
        _newPriority.value = priority
    }

    fun updateCompletedValue(completed: Long) {
        _newCompleted.value = completed
    }

    fun updateCheckedValue(checked: Boolean) {
        _newChecked.value = checked
        if (bufferTask.value?.missed == true) {
            if (checked) {
                _newMissed.value = false
                _newDeadline.value = GLOBAL_START_DATE
            } else {
                _newMissed.value = true
                _newDeadline.value = bufferTask.value?.deadline
            }
        }
    }

    fun updateTotalCheckedValue(totalChecked: Int) {
        _newTotalChecked.value = totalChecked
    }

    fun updateMissedValue(missed: Boolean) {
        _newMissed.value = missed
    }

    fun increaseTotalChecked() {
        _newChecked.value = true
        updateLastOvercheck(System.currentTimeMillis())
        updateCompletedValue(System.currentTimeMillis())
        newTotalChecked.value.let { totalChecked ->
            _newTotalChecked.value = totalChecked!!.plus(1)
        }
    }

    fun decreaseTotalChecked() {
        if (newTotalChecked.value!! > 1) {
            _newChecked.value = true
            newTotalChecked.value.let { totalChecked ->
                _newTotalChecked.value = totalChecked!!.minus(1)
            }
        } else {
            _newTotalChecked.value = 0
            _newChecked.value = false
            viewModelScope.launch {
                delay(200)
                _newCompleted.value = GLOBAL_START_DATE
            }
        }
        _newLastOvercheck.value = GLOBAL_START_DATE
    }

    fun updateRepeatModeValue(repeatMode: Int) {
        _newRepeatMode.value = repeatMode
    }

    fun updateLastOvercheck(lastOvercheck: Long) {
        _newLastOvercheck.value = lastOvercheck
    }

    fun saveState(bufferedTask: TaskType, title: String, desc: String, checked: Boolean) {
        savedStateHandle.apply {
            set(BUFFER_TASK, bufferedTask)
            set(NEW_TITLE, title)
            set(NEW_DESCRIPTION, desc)
            set(NEW_TIME, newTime.value)
            set(NEW_REPEAT_MODE, newRepeatMode.value)
            set(NEW_DEADLINE, newDeadline.value)
            set(NEW_PRIORITY, newPriority.value)
            set(NEW_COMPLETED, newCompleted.value)
            set(NEW_CHECKED, checked)
            set(NEW_TOTAL_CHECKED, newTotalChecked.value)
            set(NEW_MISSED, newMissed.value)
            set(NEW_LAST_OVERCHECK, newLastOvercheck.value)
        }
    }

    fun retrieveState(): Triple<String, String, Boolean> {
        savedStateHandle.apply {
            setBufferTask(getLiveData<TaskType>(BUFFER_TASK).value!!)
            updateTimeValue(getLiveData<Long>(NEW_TIME).value!!)
            updateDeadlineValue(getLiveData<Long>(NEW_DEADLINE).value!!)
            updatePriorityValue(getLiveData<Int>(NEW_PRIORITY).value!!)
            updateRepeatModeValue(getLiveData<Int>(NEW_REPEAT_MODE).value!!)
            updateCheckedValue(getLiveData<Boolean>(NEW_CHECKED).value!!)
            updateTotalCheckedValue(getLiveData<Int>(NEW_TOTAL_CHECKED).value!!)
            updateCompletedValue(
                getLiveData<Long>(NEW_COMPLETED).value!!
            )
            updateMissedValue(getLiveData<Boolean>(NEW_MISSED).value!!)
            updateLastOvercheck(getLiveData<Long>(NEW_LAST_OVERCHECK).value!!)

            return Triple(
                getLiveData<String>(NEW_TITLE).value!!,
                getLiveData<String>(NEW_DESCRIPTION).value!!,
                getLiveData<Boolean>(NEW_CHECKED).value!!

            )
        }
    }


    companion object {
        private const val BUFFER_TASK = "bufferTask"
        private const val NEW_TITLE = "newTitle"
        private const val NEW_DESCRIPTION = "newDescription"
        private const val NEW_CHECKED = "newChecked"
        private const val NEW_TIME = "newTime"
        private const val NEW_TOTAL_CHECKED = "newTotalChecked"
        private const val NEW_REPEAT_MODE = "newRepeatMode"
        private const val NEW_DEADLINE = "newDeadline"
        private const val NEW_PRIORITY = "newPriority"
        private const val NEW_COMPLETED = "newCompleted"
        private const val NEW_MISSED = "newMissed"
        private const val NEW_LAST_OVERCHECK = "newLastOvercheck"
    }
}