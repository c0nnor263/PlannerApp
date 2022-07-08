package com.conboi.plannerapp.data.source.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.PREMIUM_PREFERENCES
import com.conboi.plannerapp.utils.PremiumType
import com.conboi.plannerapp.utils.SynchronizationState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.premiumPreferences by preferencesDataStore(name = PREMIUM_PREFERENCES)

data class FilterPremiumPreferences(
    val isPremium: Boolean,
    val premiumType: PremiumType,
    val middleListTime: Long,
    val syncState: SynchronizationState
)

@Singleton
class PremiumPreferencesDataStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.premiumPreferences
    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->

            val isPremium = preferences[PremiumPreferencesKeys.IS_PREMIUM] ?: false

            val premiumType = PremiumType.valueOf(
                preferences[PremiumPreferencesKeys.PREMIUM_TYPE] ?: PremiumType.STANDARD.name
            )

            val middleListTime =
                preferences[PremiumPreferencesKeys.MIDDLE_LIST_TIME] ?: GLOBAL_START_DATE

            val syncState = SynchronizationState.valueOf(
                preferences[PremiumPreferencesKeys.SYNC_STATE]
                    ?: SynchronizationState.DISABLED_SYNC.name
            )

            FilterPremiumPreferences(isPremium, premiumType, middleListTime, syncState)
        }


    //Last premium state
    suspend fun updatePremium(state: Boolean) =
        dataStore.edit { preferences ->
            preferences[PremiumPreferencesKeys.IS_PREMIUM] = state
        }


    suspend fun updatePremiumType(type: PremiumType) =
        dataStore.edit { preferences ->
            preferences[PremiumPreferencesKeys.PREMIUM_TYPE] = type.name
        }


    //Sync state
    suspend fun updateSyncState(syncState: SynchronizationState) =
        dataStore.edit { preferences ->
            preferences[PremiumPreferencesKeys.SYNC_STATE] = syncState.name
        }


    suspend fun updateMiddleListTime(time: Long) =
        dataStore.edit { preferences ->
            preferences[PremiumPreferencesKeys.MIDDLE_LIST_TIME] = time
        }


    suspend fun clear() =
        dataStore.edit { preferences ->
            preferences.clear()
        }


    object PremiumPreferencesKeys {
        val IS_PREMIUM = booleanPreferencesKey("IS_PREMIUM")
        val PREMIUM_TYPE = stringPreferencesKey("PREMIUM_TYPE")
        val MIDDLE_LIST_TIME = longPreferencesKey("MIDDLE_LIST_TIME")
        val SYNC_STATE = stringPreferencesKey("SYNC_STATE")
    }
}