package com.conboi.plannerapp.ui.main.task

import android.content.Context
import androidx.lifecycle.*
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.data.source.local.repo.AppSettingsRepository
import com.conboi.plannerapp.data.source.local.repo.TaskRepository
import com.conboi.plannerapp.data.source.local.repo.UserRepository
import com.conboi.plannerapp.di.IODispatcher
import com.conboi.plannerapp.interfaces.UpdateTotalTaskCallback
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.Priority
import com.conboi.plannerapp.utils.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    appSettingsRepository: AppSettingsRepository,
    private val savedState: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TaskDetailEvent?>(null)
    val uiState = _uiState

    val reminderState = appSettingsRepository.getReminderModeState()

    private var _initialTask = MutableLiveData<TaskType>()
    val initialTask: LiveData<TaskType> = _initialTask

    private var _newTitle = MutableLiveData<String>()
    val newTitle: LiveData<String> = _newTitle

    private var _newDescription = MutableLiveData<String>()
    val newDescription: LiveData<String> = _newDescription

    private var _newTime = MutableLiveData<Long>()
    val newTime: LiveData<Long> = _newTime

    private var _newDeadline = MutableLiveData<Long>()
    val newDeadline: LiveData<Long> = _newDeadline

    private var _newPriority = MutableLiveData<Priority>()
    val newPriority: LiveData<Priority> = _newPriority

    private var _newCompleted = MutableLiveData<Long>()
    val newCompleted: LiveData<Long> = _newCompleted

    private var _newChecked = MutableLiveData<Boolean>()
    val newChecked: LiveData<Boolean> = _newChecked

    private var _newRepeatMode = MutableLiveData<RepeatMode>()
    val newRepeatMode: LiveData<RepeatMode> = _newRepeatMode

    private var _newTotalChecked = MutableLiveData<Int>()
    val newTotalChecked: LiveData<Int> = _newTotalChecked

    private var _newMissed = MutableLiveData<Boolean>()
    val newMissed: LiveData<Boolean> = _newMissed

    private var _newLastOvercheck = MutableLiveData<Long>()
    val newLastOvercheck: LiveData<Long> = _newLastOvercheck

    fun setInitialTask(task: TaskType) {
        _initialTask.value = task
        _newTitle.value = task.title
        _newDescription.value = task.description
        _newTime.value = task.time
        _newTotalChecked.value = task.totalChecked
        _newChecked.value = task.checked
        _newDeadline.value = task.deadline
        _newPriority.value = task.priority
        _newRepeatMode.value = task.repeatMode
        _newCompleted.value = task.completed
        _newMissed.value = task.missed
        _newLastOvercheck.value = task.lastOvercheck
    }

    fun updateTitleValue(title: String) {
        _newTitle.value = title
    }

    fun updateDescriptionValue(description: String) {
        _newDescription.value = description
    }

    fun updateTimeValue(time: Long) {
        _newTime.value = time
    }

    fun updateDeadlineValue(time: Long) {
        _newDeadline.value = time
    }

    fun updatePriorityValue(priority: Priority) {
        _newPriority.value = priority
    }

    fun updateMissedValue(missed: Boolean) {
        _newMissed.value = missed
    }

    fun updateRepeatModeValue(repeatMode: RepeatMode) {
        _newRepeatMode.value = repeatMode
    }

    private fun updateCompletedValue(completed: Long) {
        _newCompleted.value = completed
    }

    private fun updateTotalCheckedValue(totalChecked: Int) {
        _newTotalChecked.value = totalChecked
    }

    private fun updateLastOvercheck(lastOvercheck: Long) {
        _newLastOvercheck.value = lastOvercheck
    }

    fun updateCheckedValue(checked: Boolean) {
        _newChecked.value = checked
        if (checked) increaseTotalChecked() else decreaseTotalChecked()

        if (initialTask.value?.missed == true) {
            if (checked) {
                _newMissed.value = false
                _newDeadline.value = GLOBAL_START_DATE
            } else {
                _newMissed.value = true
                _newDeadline.value = initialTask.value?.deadline
            }
        }
    }

    private fun increaseTotalChecked() {
        _newChecked.value = true
        if (_newTitle.value?.isNotBlank() == true) {
            updateLastOvercheck(System.currentTimeMillis())
            updateCompletedValue(System.currentTimeMillis())
            newTotalChecked.value.let { totalChecked ->
                _newTotalChecked.value = totalChecked!!.plus(1)
            }
        }
    }

    private fun decreaseTotalChecked() {
        if (newTotalChecked.value!! > 1) {
            _newChecked.value = true
            newTotalChecked.value.let { totalChecked ->
                _newTotalChecked.value = totalChecked!!.minus(1)
            }
        } else {
            _newChecked.value = false
            _newTotalChecked.value = 0
            viewModelScope.launch {
                // Delay for UI changes
                delay(200)
                _newCompleted.value = GLOBAL_START_DATE
            }
        }
        _newLastOvercheck.value = GLOBAL_START_DATE
    }


    fun removeReminder() {
        updateTimeValue(GLOBAL_START_DATE)
        updateRepeatModeValue(RepeatMode.Once)
    }

    fun removeDeadline() {
        updateDeadlineValue(GLOBAL_START_DATE)
        updateMissedValue(false)
    }

    fun updateTask(context: Context, callbackTotalTask: UpdateTotalTaskCallback) =
        viewModelScope.launch {
            withContext(ioDispatcher) {
                taskRepository.updateTask(composeNewTask())
            }
        }.invokeOnCompletion {
            taskRepository.updateNewTotal(
                context,
                initialTask.value!!,
                newTotalChecked.value!!,
                newTime.value!!,
                newRepeatMode.value!!,
                newDeadline.value!!,
                callbackTotalTask
            )
        }

    private fun composeNewTask() =
        initialTask.value!!.copy(
            title = newTitle.value!!,
            description = newDescription.value!!,
            time = newTime.value!!,
            repeatMode = newRepeatMode.value!!,
            deadline = newDeadline.value!!,
            priority = newPriority.value!!,
            checked = newChecked.value!!,
            totalChecked = newTotalChecked.value!!,
            completed = newCompleted.value!!,
            missed = newMissed.value!!,
            lastOvercheck = newLastOvercheck.value!!
        )

    fun getTask(id: Int) = taskRepository.getTask(id)


    fun incrementTotalCompleted(differ: Int) =
        viewModelScope.launch { userRepository.incrementTotalCompleted(differ) }


    fun decrementTotalCompleted(differ: Int) =
        viewModelScope.launch { userRepository.decrementTotalCompleted(differ) }

    fun isEdited(): Boolean {
        initialTask.value?.let { task ->
            return newTitle.value != task.title ||
                    newDescription.value != task.description ||
                    newChecked.value != task.checked ||
                    newTime.value != task.time ||
                    newRepeatMode.value != task.repeatMode ||
                    newDeadline.value != task.deadline ||
                    newPriority.value != task.priority ||
                    newCompleted.value != task.completed ||
                    newTotalChecked.value != task.totalChecked
        }
        return false
    }


    fun sendCancelExitEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) TaskDetailEvent.ShowCancelExit else null
    }

    fun sendSetTimeReminderEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) TaskDetailEvent.ShowSetTimeReminder else null
    }

    fun sendSetTimeDeadlineEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) TaskDetailEvent.ShowSetTimeDeadline else null
    }


    fun saveState() {
        savedState.apply {
            set(INITIAL_TASK, initialTask.value)
            set(NEW_TITLE, newTitle.value)
            set(NEW_DESCRIPTION, newDescription.value)
            set(NEW_TIME, newTime.value)
            set(NEW_REPEAT_MODE, newRepeatMode.value)
            set(NEW_DEADLINE, newDeadline.value)
            set(NEW_PRIORITY, newPriority.value)
            set(NEW_COMPLETED, newCompleted.value)
            set(NEW_CHECKED, newChecked.value)
            set(NEW_TOTAL_CHECKED, newTotalChecked.value)
            set(NEW_MISSED, newMissed.value)
            set(NEW_LAST_OVERCHECK, newLastOvercheck.value)
        }
    }

    fun retrieveState() {
        savedState.apply {
            getLiveData<TaskType>(INITIAL_TASK).value?.let { setInitialTask(it) }
            getLiveData<String>(NEW_TITLE).value?.let { updateTitleValue(it) }
            getLiveData<String>(NEW_DESCRIPTION).value?.let { updateDescriptionValue(it) }
            getLiveData<Long>(NEW_TIME).value?.let { updateTimeValue(it) }
            getLiveData<Long>(NEW_DEADLINE).value?.let { updateDeadlineValue(it) }
            getLiveData<Priority>(NEW_PRIORITY).value?.let { updatePriorityValue(it) }
            getLiveData<RepeatMode>(NEW_REPEAT_MODE).value?.let { updateRepeatModeValue(it) }
            getLiveData<Boolean>(NEW_CHECKED).value?.let { updateCheckedValue(it) }
            getLiveData<Int>(NEW_TOTAL_CHECKED).value?.let { updateTotalCheckedValue(it) }
            getLiveData<Long>(NEW_COMPLETED).value?.let { updateCompletedValue(it) }
            getLiveData<Boolean>(NEW_MISSED).value?.let { updateMissedValue(it) }
            getLiveData<Long>(NEW_LAST_OVERCHECK).value?.let { updateLastOvercheck(it) }
        }
    }

    sealed class TaskDetailEvent {
        object ShowCancelExit : TaskDetailEvent()
        object ShowSetTimeReminder : TaskDetailEvent()
        object ShowSetTimeDeadline : TaskDetailEvent()
    }

    companion object {
        private const val INITIAL_TASK = "initialTask"
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