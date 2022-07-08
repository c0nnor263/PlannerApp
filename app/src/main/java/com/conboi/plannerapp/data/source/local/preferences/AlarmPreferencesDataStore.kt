package com.conboi.plannerapp.data.source.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.conboi.plannerapp.utils.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

const val ALARM_IS_NOT_EMPTY = "AlarmIsNotEmpty"

val Context.alarmPreferences by preferencesDataStore(name = ALARM_PREFERENCES)

/** There is no more FilterPreferences
 *  Map's keys are using unique name
 */

@Singleton
class AlarmPreferencesDataStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.alarmPreferences
    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }


    suspend fun addReminderPref(id: Int, newReminderTime: Long) =
        dataStore.edit { preferences ->
            val alarmIsNotEmptyKey = booleanPreferencesKey(ALARM_IS_NOT_EMPTY)

            val alarmIdKey = stringPreferencesKey(id.toString())
            val deadlineTime = getTimeFromString(preferences[alarmIdKey], AlarmType.DEADLINE)

            preferences[alarmIsNotEmptyKey] = true
            preferences[alarmIdKey] = "$newReminderTime:$deadlineTime"
        }

    suspend fun addDeadlinePref(id: Int, newDeadlineTime: Long) =
        dataStore.edit { preferences ->
            val alarmIsNotEmptyKey = booleanPreferencesKey(ALARM_IS_NOT_EMPTY)

            val alarmIdKey = stringPreferencesKey(id.toString())
            val reminderTime =
                getTimeFromString(preferences[alarmIdKey], AlarmType.REMINDER)

            preferences[alarmIsNotEmptyKey] = true
            preferences[alarmIdKey] = "$reminderTime:$newDeadlineTime"
        }


    suspend fun removeReminderPref(id: Int) =
        dataStore.edit { preferences ->
            val alarmIsNotEmptyKey = booleanPreferencesKey(ALARM_IS_NOT_EMPTY)

            val alarmIdKey = stringPreferencesKey(id.toString())
            val deadlineTime = getTimeFromString(preferences[alarmIdKey], AlarmType.DEADLINE)

            if (deadlineTime == GLOBAL_START_DATE.toString() || deadlineTime.isBlank()) {
                preferences.remove(alarmIdKey)
                preferences[alarmIsNotEmptyKey] = false
            } else {
                preferences[alarmIdKey] =
                    getStringTimeWithReset(resetFor = AlarmType.REMINDER, deadlineTime)
            }
        }


    suspend fun removeDeadlinePref(id: Int) =
        dataStore.edit { preferences ->
            val alarmIsNotEmptyKey = booleanPreferencesKey(ALARM_IS_NOT_EMPTY)

            val alarmIdKey = stringPreferencesKey(id.toString())
            val reminderTime = getTimeFromString(preferences[alarmIdKey], AlarmType.REMINDER)

            if (reminderTime == GLOBAL_START_DATE.toString() || reminderTime.isBlank()) {
                preferences.remove(alarmIdKey)
                preferences[alarmIsNotEmptyKey] = false
            } else {
                preferences[alarmIdKey] =
                    getStringTimeWithReset(resetFor = AlarmType.DEADLINE, reminderTime)
            }
        }

    suspend fun clear() =
        dataStore.edit { preferences ->
            preferences.clear()
        }
}