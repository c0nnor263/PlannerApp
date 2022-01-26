package com.conboi.plannerapp.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class SortOrder { BY_TITLE, BY_DATE, BY_COMPLETE, BY_OVERCOMPLETED }
enum class SynchronizationState { PENDING_SYNC, COMPLETE_SYNC, ERROR_SYNC, DISABLED_SYNC }
enum class PremiumType { STANDARD, MONTH, SIX_MONTH, YEAR }

val Context.dataStore by preferencesDataStore("user_preferences")

data class FilterPreferences(
    val premiumState: Boolean,
    val premiumType: PremiumType,
    val middleListTime: Long,
    val rateUsCount: Int,
    val syncState: SynchronizationState,
    val sortOrder: SortOrder,
    val hideCompleted: Boolean,
    val hideOvercompleted: Boolean,
    val totalCompleted: Int,
    val privateModeState: Boolean,
    val vibrationModeState: Boolean,
    val remindersModeState: Boolean,
    val notificationsModeState: Boolean
)

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore
    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.d("Preferences Manager", "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val premiumState = preferences[PreferencesKeys.PREMIUM_STATE] ?: false

            val premiumType = PremiumType.valueOf(
                preferences[PreferencesKeys.PREMIUM_TYPE] ?: PremiumType.STANDARD.name
            )

            val middleListTime = preferences[PreferencesKeys.MIDDLE_LIST_TIME] ?: GLOBAL_START_DATE

            val rateUsCount = preferences[PreferencesKeys.RATE_US_COUNT] ?: 0

            val syncState = SynchronizationState.valueOf(
                preferences[PreferencesKeys.SYNC_STATE] ?: SynchronizationState.DISABLED_SYNC.name
            )
            val sortOrder = SortOrder.valueOf(
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_DATE.name
            )
            val hideCompleted = preferences[PreferencesKeys.HIDE_COMPLETED] ?: false

            val hideOverCompleted = preferences[PreferencesKeys.HIDE_OVERCOMPLETED] ?: false

            val totalCompleted = preferences[PreferencesKeys.TOTAL_COMPLETED] ?: 0

            val privateModeState = preferences[PreferencesKeys.PRIVATE_MODE] ?: false

            val vibrationModeState = preferences[PreferencesKeys.VIBRATION_MODE] ?: true

            val remindersModeState = preferences[PreferencesKeys.REMINDERS_MODE] ?: true

            val notificationsModeState = preferences[PreferencesKeys.NOTIFICATIONS_MODE] ?: true

            FilterPreferences(
                premiumState,
                premiumType,
                middleListTime,
                rateUsCount,
                syncState,
                sortOrder,
                hideCompleted,
                hideOverCompleted,
                totalCompleted,
                privateModeState,
                vibrationModeState,
                remindersModeState,
                notificationsModeState
            )
        }

    //Last premium state
    suspend fun updatePremium(state: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREMIUM_STATE] = state
        }
    }

    suspend fun updatePremiumType(type: PremiumType) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREMIUM_TYPE] = type.name
        }
    }

    //Sync state
    suspend fun updateSyncState(syncState: SynchronizationState) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_STATE] = syncState.name
        }
    }

    //Sort order
    suspend fun updateSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    //Hide completed
    suspend fun updateHideCompleted(hideCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_COMPLETED] = hideCompleted
        }
    }

    //Hide overcompleted
    suspend fun updateHideOvercompleted(hideOverCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_OVERCOMPLETED] = hideOverCompleted
        }
    }

    //Total completed
    suspend fun updateTotalCompleted(totalCompleted: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_COMPLETED] = totalCompleted
        }
    }

    suspend fun incrementTotalCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_COMPLETED] =
                preferencesFlow.first().totalCompleted.plus(1)
        }
    }

    suspend fun incrementTotalCompleted(differ: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_COMPLETED] =
                preferencesFlow.first().totalCompleted.plus(differ)
        }
    }

    suspend fun decrementTotalCompleted() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_COMPLETED] =
                preferencesFlow.first().totalCompleted.minus(1)
        }
    }

    suspend fun decrementTotalCompleted(differ: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_COMPLETED] =
                preferencesFlow.first().totalCompleted.minus(differ)
        }
    }

    //Settings fragment
    suspend fun updatePrivateModeState(privateState: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIVATE_MODE] = privateState
        }
    }

    suspend fun updateVibrationModeState(vibrationState: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATION_MODE] = vibrationState
        }
    }

    suspend fun updateRemindersModeState(remindersState: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDERS_MODE] = remindersState
        }
    }

    suspend fun updateNotificationsModeState(notificationsState: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_MODE] = notificationsState
        }
    }


    suspend fun updateRateUs(count: Int?) {
        dataStore.edit { preferences ->
            if (count != null) {
                preferences[PreferencesKeys.RATE_US_COUNT] = count
            } else {
                preferences[PreferencesKeys.RATE_US_COUNT] =
                    preferencesFlow.first().rateUsCount.plus(1)
            }
        }
    }

    suspend fun updateMiddleListTime(time: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MIDDLE_LIST_TIME] = time
        }
    }

    suspend fun signOut() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }


    object PreferencesKeys {
        val PREMIUM_STATE = booleanPreferencesKey("PREMIUM_STATE")
        val PREMIUM_TYPE = stringPreferencesKey("PREMIUM_TYPE")
        val MIDDLE_LIST_TIME = longPreferencesKey("MIDDLE_LIST_TIME")
        val SYNC_STATE = stringPreferencesKey("SYNC_STATE")
        val SORT_ORDER = stringPreferencesKey("SORT_ORDER")
        val HIDE_COMPLETED = booleanPreferencesKey("HIDE_COMPLETED")
        val HIDE_OVERCOMPLETED = booleanPreferencesKey("HIDE_OVERCOMPLETED")
        val TOTAL_COMPLETED = intPreferencesKey("TOTAL_COMPLETED")
        val PRIVATE_MODE = booleanPreferencesKey("PRIVATE_MODE")
        val VIBRATION_MODE = booleanPreferencesKey("VIBRATION_MODE")
        val REMINDERS_MODE = booleanPreferencesKey("REMINDERS_MODE")
        val NOTIFICATIONS_MODE = booleanPreferencesKey("NOTIFICATIONS_MODE")
        val RATE_US_COUNT = intPreferencesKey("RATE_US_COUNT")
    }
}