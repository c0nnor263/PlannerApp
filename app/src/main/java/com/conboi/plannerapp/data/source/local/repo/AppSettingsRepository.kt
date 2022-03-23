package com.conboi.plannerapp.data.source.local.repo

import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.asLiveData
import com.conboi.plannerapp.data.source.local.preferences.ALARM_IS_NOT_EMPTY
import com.conboi.plannerapp.data.source.local.preferences.AlarmPreferencesDataStore
import com.conboi.plannerapp.data.source.local.preferences.AppPreferencesDataStore
import com.conboi.plannerapp.data.source.local.preferences.UserSettingsPreferencesDataStore
import com.conboi.plannerapp.di.ActivitySharedPref
import com.conboi.plannerapp.utils.LANGUAGE
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject


@Module
@InstallIn(ActivityRetainedComponent::class)
class AppSettingsRepository @Inject constructor(
    private val appPreferencesDataStore: AppPreferencesDataStore,
    private val alarmPreferencesDataStore: AlarmPreferencesDataStore,
    private val userSettingsPreferencesDataStore: UserSettingsPreferencesDataStore,
    @ActivitySharedPref val activitySharedPref: SharedPreferences
) {
    private val appPreferencesFlow = appPreferencesDataStore.preferencesFlow
    private val alarmPreferencesFlow = alarmPreferencesDataStore.preferencesFlow
    private val userSettingsPreferencesFlow = userSettingsPreferencesDataStore.preferencesFlow


    suspend fun getLastUserIdValue() = appPreferencesFlow.first().lastUserId

    suspend fun getIsFirstLaunchValue() = appPreferencesFlow.first().isFirstLaunch

    suspend fun getIsImportDownloadedValue() = appPreferencesFlow.first().isImportDownloaded

    suspend fun getImportConfirmValue() = appPreferencesFlow.first().isImportConfirmed

    suspend fun getEmailConfirmDialogValue() = appPreferencesFlow.first().isEmailConfirmShowed

    suspend fun getIsResubscribeShowedValue() = appPreferencesFlow.first().isResubscribeShowed


    suspend fun getPrivateModeStateValue() = userSettingsPreferencesFlow.first().privateState

    fun getPrivateModeState() = combine(userSettingsPreferencesFlow) { it.first().privateState}.asLiveData()

    fun getVibrationModeState() = combine(userSettingsPreferencesFlow) { it.first().vibrationState }.asLiveData()

    fun getReminderModeState() = combine(userSettingsPreferencesFlow) { it.first().reminderState }.asLiveData()

    fun getNotificationModeState() = combine(userSettingsPreferencesFlow) { it.first().notificationState }.asLiveData()


    suspend fun getAlarmIsNotEmpty() = alarmPreferencesFlow.first()[booleanPreferencesKey(
        ALARM_IS_NOT_EMPTY
    )] ?: false

    fun getCurrentLanguage() = activitySharedPref.getString(LANGUAGE, Locale.getDefault().language)


    suspend fun updateLastUserId(lastUserId: String) =
        appPreferencesDataStore.updateLastUserId(lastUserId)

    suspend fun updateImportConfirmed(state: Boolean) =
        appPreferencesDataStore.updateImportConfirmed(state)

    suspend fun updateFirstLaunch(state: Boolean) =
        appPreferencesDataStore.updateFirstLaunch(state)

    suspend fun updateEmailConfirmShowed(state: Boolean) =
        appPreferencesDataStore.updateEmailConfirmShowed(state)

    suspend fun updateResubscribeShowed(state: Boolean) =
        appPreferencesDataStore.updateResubscribeShowed(state)


    suspend fun updatePrivateState(state: Boolean) =
        userSettingsPreferencesDataStore.updatePrivateState(state)

    suspend fun updateVibrationState(state: Boolean) =
        userSettingsPreferencesDataStore.updateVibrationState(state)

    suspend fun updateReminderState(state: Boolean) =
        userSettingsPreferencesDataStore.updateReminderState(state)

    suspend fun updateNotificationState(state: Boolean) =
        userSettingsPreferencesDataStore.updateNotificationState(state)


    fun updateAppLanguage(newLanguage: String) =
        activitySharedPref.edit().putString(LANGUAGE, newLanguage).apply()


    suspend fun successImport() {
        appPreferencesDataStore.updateImportConfirmed(true)
        appPreferencesDataStore.updateImportDownloaded(true)
    }

    suspend fun clearPreferences() {
        appPreferencesDataStore.clear()
        alarmPreferencesDataStore.clear()
        userSettingsPreferencesDataStore.clear()
        activitySharedPref.edit().clear().apply()
    }

    suspend fun isAlarmFileEmpty() = alarmPreferencesFlow.first().asMap().isNotEmpty()
}