package com.conboi.plannerapp.ui.main

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.data.source.local.repo.AppSettingsRepository
import com.conboi.plannerapp.data.source.local.repo.TaskRepository
import com.conboi.plannerapp.data.source.local.repo.UserRepository
import com.conboi.plannerapp.data.source.remote.repo.FirebaseRepository
import com.conboi.plannerapp.di.IODispatcher
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.shared.firebase.FirebaseUserLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository,
    private val firebaseRepository: FirebaseRepository,
    private val appSettingsRepository: AppSettingsRepository,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MainFragmentEvent?>(null)
    val uiState = _uiState

    val authState = FirebaseUserLiveData().map {
        if (it != null) {
            FirebaseUserLiveData.AuthenticationState.AUTHENTICATED
        } else {
            FirebaseUserLiveData.AuthenticationState.UNAUTHENTICATED
        }
    }

    val premiumState = userRepository.getPremiumState()

    val hideOvercompleted = userRepository.getHideOvercompleted()

    val hideCompleted = userRepository.getHideCompleted()

    val middleListTime: LiveData<Long> = userRepository.getMiddleListTime()

    //Sizes
    val taskSize: LiveData<Int> = taskRepository.getTaskSize()
    val allCompletedTaskSize: LiveData<Int> = taskRepository.getCompletedTaskCount()
    val allOvercompletedTaskSize: LiveData<Int> = taskRepository.getOvercompletedTaskCount()

    val sortedTasks = taskRepository.getSortedTasks()


    fun getUser() = userRepository.getUser()

    fun onUndoDeleteClick(context: Context, task: TaskType) =
        viewModelScope.launch {
            withContext(ioDispatcher) {
                taskRepository.onUndoDeleteClick(context, task)
            }
        }

    fun increaseRateUs(count: Int = 1) = repeat(count) {
        viewModelScope.launch {
            val rateUsCount = userRepository.getRateUsCount()
            if (rateUsCount < 15) {
                userRepository.increaseRateUs()
            } else if (rateUsCount == 15) {
                sendRateUsEvent()
            }
        }
    }

    fun resetRateUs() = viewModelScope.launch {
        userRepository.resetRateUs()
    }

    fun neverShowRateUs() = viewModelScope.launch {
        userRepository.neverShowRateUs()
    }

    fun onTaskCheckedChanged(
        task: TaskType,
        checked: Boolean,
        increase: Boolean
    ) = viewModelScope.launch {
        withContext(ioDispatcher) {
            taskRepository.onTaskCheckedChanged(task, checked, increase)
        }
    }

    fun onTitleChanged(task: TaskType, title: String) = viewModelScope.launch {
        withContext(ioDispatcher) {
            taskRepository.onTitleChanged(task, title)
        }
    }

    fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
        withContext(ioDispatcher) {
            taskRepository.onHideCompletedClick(hideCompleted)
        }
    }

    fun onHideOvercompletedClick(hideOvercompleted: Boolean) = viewModelScope.launch {
        withContext(ioDispatcher) {
            taskRepository.onHideOvercompletedClick(hideOvercompleted)
        }
    }


    private fun updateSyncState(sync: SynchronizationState) =
        viewModelScope.launch {
            userRepository.updateSyncState(sync)
        }

    fun uploadTasksWithBackup(list: List<TaskType>) {
        if (getUser()?.isEmailVerified == true) {
            viewModelScope.launch {
                withContext(ioDispatcher) {
                    updateSyncState(
                        SynchronizationState.PENDING_SYNC
                    )

                    val isBackupDownloaded = appSettingsRepository.getIsImportDownloadedValue()

                    val resultTasks = firebaseRepository.uploadTasks(
                        list,
                        isBackupDownloaded,
                        withBackupUpload = true
                    )
                    updateSyncState(
                        if (resultTasks.error == null) {
                            SynchronizationState.COMPLETE_SYNC
                        } else {
                            SynchronizationState.ERROR_SYNC
                        }
                    )
                }
            }
        } else {
            updateSyncState(
                SynchronizationState.DISABLED_SYNC
            )
        }
    }

    private fun updateMiddleList(
        downloadMiddleTime: Long = System.currentTimeMillis(),
        downloadUpdate: Boolean = false
    ) {
        if (taskSize.value!! + 1 == MIDDLE_COUNT || downloadUpdate) {
            viewModelScope.launch {
                withContext(ioDispatcher) {
                    userRepository.updateMiddleList(downloadMiddleTime)
                }
            }
        }
    }

    //Insert tasks
    fun insertTask() =
        viewModelScope.launch(ioDispatcher) {
            withContext(ioDispatcher) {
                taskRepository.insertTask()
                updateMiddleList()
            }
        }

    fun insertMultiTasks(
        inputAmountString: String,
        callback: (Int?, Exception?) -> Unit
    ) =
        viewModelScope.launch {
            val maxTaskCount =
                MAX_TASK_COUNT + if (premiumState.value == true) 50 else 0
            var enteredCount = inputAmountString.toInt()

            if (inputAmountString.isNotEmpty()) {
                if (isListNotFull()) {
                    if (enteredCount in 1..MAX_TASK_COUNT) {
                        if (enteredCount.plus(taskSize.value!!) > maxTaskCount
                        ) {
                            enteredCount = maxTaskCount - taskSize.value!!
                        }
                        callback(enteredCount, null)
                    } else {
                        callback(null, Exception(InsertMultipleTaskError.INCORRECT.name))
                    }
                } else {
                    callback(null, Exception(InsertMultipleTaskError.MAXIMUM.name))
                }
            }

            repeat(enteredCount) {
                taskRepository.insertTask()
            }
            updateMiddleList()
        }

    fun downloadTasks(
        context: Context,
        callback: (Any?, Exception?) -> Unit
    ) =
        viewModelScope.launch {
            val result =
                firebaseRepository.downloadUserTasks(context, sortedTasks.value ?: return@launch)
            if (result.error == null) {
                val downloadTasks = result.data!!
                updateMiddleList(
                    if (downloadTasks.size > 50) downloadTasks[49].created else GLOBAL_START_DATE
                )
                updateSyncState(SynchronizationState.COMPLETE_SYNC)

                callback(null, null)

                withContext(ioDispatcher) {
                    taskRepository.insertDownloadedTasks(downloadTasks)
                }


            } else {
                callback(null, result.error)
            }
        }

    fun isListNotFull(): Boolean =
        taskSize.value!! < MAX_TASK_COUNT + if (premiumState.value == true) MIDDLE_COUNT else 0

    fun downloadLatestBackupInfo(
        callback: (Long?, Exception?) -> Unit
    ) {
        viewModelScope.launch {
            val isBackupDownloaded = appSettingsRepository.getImportConfirmValue()
            val result = firebaseRepository.downloadLatestBackupInfo(isBackupDownloaded)

            if (result.error == null) {
                callback(result.data, null)
            } else {
                callback(null, result.error)
            }
        }
    }


    fun successImport() = viewModelScope.launch {
        withContext(ioDispatcher) {
            appSettingsRepository.successImport()
        }
    }

    fun initUser() = viewModelScope.launch {
        val isEmailVerified = getUser()?.isEmailVerified == true
        val importConfirmShowed = appSettingsRepository.getImportConfirmValue()
        val emailConfirmDialogShowed = appSettingsRepository.getEmailConfirmDialogValue()

        // Confirm email dialog
        if (emailConfirmDialogShowed.not()) {
            updateSyncState(
                if (isEmailVerified.not()) {
                    sendEmailNotConfirmedEvent()
                    SynchronizationState.DISABLED_SYNC
                } else {
                    SynchronizationState.COMPLETE_SYNC
                }
            )
            appSettingsRepository.updateEmailConfirmShowed(true)
        }
        if (importConfirmShowed.not() && isEmailVerified) sendImportServerTasksEvent()
    }


    fun updateImportConfirm(state: Boolean) =
        viewModelScope.launch {
            withContext(ioDispatcher) {
                appSettingsRepository.updateImportConfirmed(state)
            }
        }


    fun sendSwipeDeleteTaskEvent(
        context: Context?,
        deletedTask: TaskType?,
        show: Boolean? = true
    ) =
        viewModelScope.launch {
            withContext(ioDispatcher) {
                context?.let {
                    taskRepository.swipeDeleteTask(context, deletedTask!!)
                }

                _uiState.value =
                    if (show == true) MainFragmentEvent.SwipeDeleteTask(deletedTask!!) else null

            }
        }

    fun sendErrorImportTaskEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) MainFragmentEvent.ShowErrorImportTask else null
    }

    fun sendErrorCheckingTaskEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) MainFragmentEvent.ShowErrorCheckingTask else null
    }

    fun sendErrorMaxTaskEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) MainFragmentEvent.ShowErrorMaxTask else null
    }

    fun sendExitEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) MainFragmentEvent.ShowExit else null
    }

    fun sendRateUsEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) MainFragmentEvent.ShowRate else null
    }

    fun sendImportServerTasksEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) MainFragmentEvent.ShowImportServerTasks else null
    }

    fun sendSyncEvent(premium: Boolean?, lastSyncString: String?, show: Boolean? = true) {
        updateImportConfirm(true)
        _uiState.value = if (show == true && premium == true) {
            MainFragmentEvent.ShowSync(
                premium,
                lastSyncString!!
            )
        } else {
            null
        }
    }

    fun sendCantOvercheckEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) MainFragmentEvent.ShowCantOvercheck else null
    }

    fun sendCantCheckEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) MainFragmentEvent.ShowCantCheck else null
    }

    fun sendEmailNotConfirmedEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) MainFragmentEvent.ShowEmailNotConfirmed else null
    }

    sealed class MainFragmentEvent {
        data class ShowSync(val premium: Boolean, val lastSyncString: String) : MainFragmentEvent()
        data class SwipeDeleteTask(val task: TaskType) : MainFragmentEvent()
        object ShowImportServerTasks : MainFragmentEvent()
        object ShowRate : MainFragmentEvent()
        object ShowErrorImportTask : MainFragmentEvent()
        object ShowErrorCheckingTask : MainFragmentEvent()
        object ShowEmailNotConfirmed : MainFragmentEvent()
        object ShowErrorMaxTask : MainFragmentEvent()
        object ShowExit : MainFragmentEvent()
        object ShowCantOvercheck : MainFragmentEvent()
        object ShowCantCheck : MainFragmentEvent()
    }
}