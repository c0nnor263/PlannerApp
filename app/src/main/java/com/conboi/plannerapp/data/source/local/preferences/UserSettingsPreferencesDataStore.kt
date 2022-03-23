package com.conboi.plannerapp.data.source.local.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.conboi.plannerapp.utils.USER_SETTINGS_PREFERENCES
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


val Context.userSettingsPreferences by preferencesDataStore(name = USER_SETTINGS_PREFERENCES)

data class FilterUserSettingsPreferences(
    val privateState: Boolean,
    val vibrationState: Boolean,
    val reminderState: Boolean,
    val notificationState: Boolean
)

@Singleton
class UserSettingsPreferencesDataStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.userSettingsPreferences
    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.d("Preferences Manager", "Error reading $USER_SETTINGS_PREFERENCES", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->

            val privateModeState =
                preferences[UserSettingsPreferencesKeys.PRIVATE_MODE] ?: false

            val vibrationModeState =
                preferences[UserSettingsPreferencesKeys.VIBRATION_MODE] ?: true

            val reminderModeState =
                preferences[UserSettingsPreferencesKeys.REMINDER_MODE] ?: true

            val notificationModeState =
                preferences[UserSettingsPreferencesKeys.NOTIFICATION_MODE] ?: true

            FilterUserSettingsPreferences(
                privateModeState,
                vibrationModeState,
                reminderModeState,
                notificationModeState
            )
        }

    suspend fun updatePrivateState(privateState: Boolean) =
        dataStore.edit { preferences ->
            preferences[UserSettingsPreferencesKeys.PRIVATE_MODE] = privateState
        }

    suspend fun updateVibrationState(vibrationState: Boolean) =
        dataStore.edit { preferences ->
            preferences[UserSettingsPreferencesKeys.VIBRATION_MODE] = vibrationState
        }

    suspend fun updateReminderState(remindersState: Boolean) =
        dataStore.edit { preferences ->
            preferences[UserSettingsPreferencesKeys.REMINDER_MODE] = remindersState
        }

    suspend fun updateNotificationState(notificationsState: Boolean) =
        dataStore.edit { preferences ->
            preferences[UserSettingsPreferencesKeys.NOTIFICATION_MODE] = notificationsState
        }

    suspend fun clear() =
        dataStore.edit { preferences ->
            preferences.clear()
        }

    object UserSettingsPreferencesKeys {
        val PRIVATE_MODE = booleanPreferencesKey("PRIVATE_MODE")
        val VIBRATION_MODE = booleanPreferencesKey("VIBRATION_MODE")
        val REMINDER_MODE = booleanPreferencesKey("REMINDER_MODE")
        val NOTIFICATION_MODE = booleanPreferencesKey("NOTIFICATION_MODE")
    }
}