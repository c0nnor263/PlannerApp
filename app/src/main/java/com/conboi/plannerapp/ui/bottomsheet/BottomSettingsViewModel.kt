package com.conboi.plannerapp.ui.bottomsheet

import android.content.Context
import androidx.lifecycle.*
import com.conboi.plannerapp.data.source.local.repo.AppSettingsRepository
import com.conboi.plannerapp.data.source.local.repo.TaskRepository
import com.conboi.plannerapp.di.IODispatcher
import com.conboi.plannerapp.utils.AlarmType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BottomSettingsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val appSettingsRepository: AppSettingsRepository,
    private val taskRepository: TaskRepository,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val appCurrentLanguage = appSettingsRepository.getCurrentLanguage()

    private val _selectedLanguage =
        MutableLiveData(Locale(appCurrentLanguage ?: Locale.getDefault().language))
    val selectedLanguage: LiveData<Locale> = _selectedLanguage

    val privateState: LiveData<Boolean> = appSettingsRepository.getPrivateModeState()
    val vibrationState: LiveData<Boolean> = appSettingsRepository.getVibrationModeState()
    val reminderState: LiveData<Boolean> = appSettingsRepository.getReminderModeState()
    val notificationState: LiveData<Boolean> = appSettingsRepository.getNotificationModeState()


    fun getTasksSize() = taskRepository.getTaskSize()

    fun updateSelectedLanguage(newLang: Locale) {
        _selectedLanguage.value = newLang
    }

    fun updateAppLanguage() =
        viewModelScope.launch { appSettingsRepository.updateAppLanguage(selectedLanguage.value!!.language) }

    fun updatePrivateModeState() = viewModelScope.launch {
        appSettingsRepository.updatePrivateState(privateState.value!!.not())
    }

    fun updateVibrationModeState() = viewModelScope.launch {
        appSettingsRepository.updateVibrationState(vibrationState.value!!.not())
    }

    fun updateReminderModeState() = viewModelScope.launch {
        appSettingsRepository.updateReminderState(reminderState.value!!.not())
    }

    fun updateNotificationModeState() = viewModelScope.launch {
        appSettingsRepository.updateNotificationState(notificationState.value!!.not())
    }

    fun cancelAllAlarmsType(context: Context, alarmType: AlarmType) =
        taskRepository.cancelAllAlarmsType(context, alarmType)


    fun deleteAllTasks(context: Context) =
        viewModelScope.launch { withContext(ioDispatcher) { taskRepository.deleteAllTasks(context) } }


    fun isLanguageCanChange(lang: String): Boolean =
        appCurrentLanguage.equals(lang).not()

    suspend fun isReminderAvailable(): Boolean {
        val isFileInitialized = appSettingsRepository.getAlarmIsNotEmpty()
        val isFileEmpty = appSettingsRepository.isAlarmFileEmpty()
        return isFileInitialized && isFileEmpty
    }

    fun saveState() {
        savedStateHandle[SELECTED_LANGUAGE] = selectedLanguage.value
    }

    fun retrieveState() {
        savedStateHandle.getLiveData<Locale>(SELECTED_LANGUAGE).value?.let {
            updateSelectedLanguage(
                it
            )
        }
    }

    companion object {
        private const val SELECTED_LANGUAGE = "selectedLanguage"
    }
}