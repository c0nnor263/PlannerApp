package com.conboi.plannerapp.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class SortOrder { BY_TITLE, BY_DATE, BY_COMPLETE }

val Context.dataStore by preferencesDataStore("user_preferences")

data class FilterPreferences(val sortOrder: SortOrder, val hideCompleted: Boolean, val totalCompleted:Int)

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore
    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.d("Preferences Manager", "Error reading preferences", exception)
             emit(emptyPreferences())
            }else{
                throw exception
            }
        }
        .map { preferences ->
            val sortOrder = SortOrder.valueOf(
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_DATE.name
            )
            val hideCompleted = preferences[PreferencesKeys.HIDE_COMPLETED] ?: false

            val totalCompleted = preferences[PreferencesKeys.TOTAL_COMPLETED] ?:0

            FilterPreferences(sortOrder, hideCompleted, totalCompleted)
        }

    suspend fun updateSortOrder(sortOrder: SortOrder){
        dataStore.edit {preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }
    suspend fun updateHideCompleted(hideCompleted: Boolean){
        dataStore.edit {preferences ->
            preferences[PreferencesKeys.HIDE_COMPLETED] = hideCompleted
        }
    }
    suspend fun updateTotalCompleted(totalCompleted: Int){
        dataStore.edit { preferences->
            preferences[PreferencesKeys.TOTAL_COMPLETED] = totalCompleted
        }
    }
    suspend fun incrementTotalCompleted(totalCompleted: Int){
        dataStore.edit { preferences->
            preferences[PreferencesKeys.TOTAL_COMPLETED] = totalCompleted.plus(1)
        }
    }
    suspend fun decrementTotalCompleted(totalCompleted: Int){
        dataStore.edit { preferences->
            preferences[PreferencesKeys.TOTAL_COMPLETED] = totalCompleted.minus(1)
        }
    }


     object PreferencesKeys {
        val SORT_ORDER = stringPreferencesKey("SORT_ORDER")
        val HIDE_COMPLETED = booleanPreferencesKey("HIDE_COMPLETED")
        val TOTAL_COMPLETED = intPreferencesKey("TOTAL_COMPLETED")
    }
}