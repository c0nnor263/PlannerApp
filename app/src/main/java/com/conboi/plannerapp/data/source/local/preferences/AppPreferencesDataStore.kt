package com.conboi.plannerapp.data.source.local.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.conboi.plannerapp.utils.APP_PREFERENCES
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.appPreferences by preferencesDataStore(name = APP_PREFERENCES)

data class FilterAppPreferences(
    val lastUserId: String,
    val isImportConfirmed: Boolean,
    val isImportDownloaded: Boolean,
    val isFirstLaunch: Boolean,
    val isEmailConfirmShowed: Boolean,
    val isResubscribeShowed: Boolean,
)

@Singleton
class AppPreferencesDataStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.appPreferences
    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.d("Preferences Manager", "Error reading $APP_PREFERENCES", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val lastUserId = preferences[AppPreferencesKeys.LAST_USER_ID] ?: ""

            val isImportConfirmed = preferences[AppPreferencesKeys.IS_IMPORT_CONFIRMED] ?: false

            val isImportDownloaded = preferences[AppPreferencesKeys.IS_IMPORT_DOWNLOADED] ?: false

            val isFirstLaunch = preferences[AppPreferencesKeys.IS_FIRST_LAUNCH] ?: false

            val isEmailConfirmShowed =
                preferences[AppPreferencesKeys.IS_EMAIL_CONFIRM_SHOWED] ?: false

            val isResubscribeShowed =
                preferences[AppPreferencesKeys.IS_RESUBSCRIBE_SHOWED] ?: false

            FilterAppPreferences(
                lastUserId,
                isImportConfirmed,
                isImportDownloaded,
                isFirstLaunch,
                isEmailConfirmShowed,
                isResubscribeShowed
            )
        }

    suspend fun updateLastUserId(lastUserId: String) = dataStore.edit { preferences ->
        preferences[AppPreferencesKeys.LAST_USER_ID] = lastUserId
    }

    suspend fun updateEmailConfirmShowed(state: Boolean) = dataStore.edit { preferences ->
        preferences[AppPreferencesKeys.IS_EMAIL_CONFIRM_SHOWED] = state
    }

    suspend fun updateImportDownloaded(state: Boolean) = dataStore.edit { preferences ->
        preferences[AppPreferencesKeys.IS_IMPORT_DOWNLOADED] = state
    }

    suspend fun updateImportConfirmed(state: Boolean) = dataStore.edit { preferences ->
        preferences[AppPreferencesKeys.IS_IMPORT_CONFIRMED] = state
    }

    suspend fun updateFirstLaunch(state: Boolean) = dataStore.edit { preferences ->
        preferences[AppPreferencesKeys.IS_FIRST_LAUNCH] = state
    }

    suspend fun updateResubscribeShowed(state: Boolean) = dataStore.edit { preferences ->
        preferences[AppPreferencesKeys.IS_RESUBSCRIBE_SHOWED] = state
    }


    suspend fun clear() = dataStore.edit { preferences ->
        preferences.clear()
    }

    object AppPreferencesKeys {
        val LAST_USER_ID = stringPreferencesKey("LAST_USER_ID")
        val IS_IMPORT_CONFIRMED = booleanPreferencesKey("IS_IMPORT_CONFIRMED")
        val IS_IMPORT_DOWNLOADED = booleanPreferencesKey("IS_IMPORT_DOWNLOADED")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("IS_FIRST_LAUNCH")
        val IS_EMAIL_CONFIRM_SHOWED = booleanPreferencesKey("IS_EMAIL_CONFIRM_SHOWED")
        val IS_RESUBSCRIBE_SHOWED = booleanPreferencesKey("IS_RESUBSCRIBE_SHOWED")
    }
}