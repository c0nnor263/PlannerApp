package com.conboi.plannerapp.data.source.local.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.conboi.plannerapp.utils.SortOrder
import com.conboi.plannerapp.utils.USER_PREFERENCES
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.userPreferences by preferencesDataStore(name = USER_PREFERENCES)

data class FilterUserPreferences(
    val totalCompleted: Int,
    val rateUsCount: Int,
    val sortOrder: SortOrder,
    val isHideCompleted: Boolean,
    val isHideOvercompleted: Boolean
)

@Singleton
class UserPreferencesDataStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.userPreferences
    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.d("Preferences Manager", "Error reading $USER_PREFERENCES", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val totalCompleted = preferences[UserPreferencesKeys.TOTAL_COMPLETED] ?: 0

            val rateUsCount = preferences[UserPreferencesKeys.RATE_US_COUNT] ?: 0

            val sortOrder = SortOrder.valueOf(
                preferences[UserPreferencesKeys.SORT_ORDER] ?: SortOrder.BY_DATE.name
            )
            val isHideCompleted = preferences[UserPreferencesKeys.IS_HIDE_COMPLETED] ?: false

            val isHideOverCompleted = preferences[UserPreferencesKeys.IS_HIDE_OVERCOMPLETED] ?: false

            FilterUserPreferences(
                totalCompleted,
                rateUsCount,
                sortOrder,
                isHideCompleted,
                isHideOverCompleted
            )
        }

    // Total completed
    suspend fun updateTotalCompleted(totalCompleted: Int) =
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.TOTAL_COMPLETED] = totalCompleted
        }

    suspend fun incrementTotalCompleted(differ: Int = 1) =
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.TOTAL_COMPLETED] =
                preferencesFlow.first().totalCompleted.plus(differ)
        }

    suspend fun decrementTotalCompleted(differ: Int = 1) =
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.TOTAL_COMPLETED] =
                preferencesFlow.first().totalCompleted.minus(differ)
        }

    // Sort order
    suspend fun updateSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    // Hide completed
    suspend fun updateHideCompleted(hideCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.IS_HIDE_COMPLETED] = hideCompleted
        }
    }

    // Hide overcompleted
    suspend fun updateHideOvercompleted(hideOverCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.IS_HIDE_OVERCOMPLETED] = hideOverCompleted
        }
    }

    suspend fun updateRateUs(count: Int) {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.RATE_US_COUNT] = count
        }
    }

    suspend fun increaseRateUs() {
        dataStore.edit { preferences ->
            preferences[UserPreferencesKeys.RATE_US_COUNT] =
                preferencesFlow.first().rateUsCount.plus(1)
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    object UserPreferencesKeys {
        val TOTAL_COMPLETED = intPreferencesKey("TOTAL_COMPLETED")
        val SORT_ORDER = stringPreferencesKey("SORT_ORDER")
        val RATE_US_COUNT = intPreferencesKey("RATE_US_COUNT")
        val IS_HIDE_COMPLETED = booleanPreferencesKey("IS_HIDE_COMPLETED")
        val IS_HIDE_OVERCOMPLETED = booleanPreferencesKey("IS_HIDE_OVERCOMPLETED")
    }
}