package com.conboi.plannerapp.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import com.conboi.plannerapp.data.source.local.repo.AppSettingsRepository
import com.conboi.plannerapp.data.source.local.repo.UserRepository
import com.google.firebase.auth.FirebaseUser
import com.qonversion.android.sdk.QUserProperties
import com.qonversion.android.sdk.Qonversion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {
    suspend fun isFirstLaunch() = appSettingsRepository.getIsFirstLaunchValue()

    suspend fun checkForNewUser(newUser: FirebaseUser, context: Context) {
        userRepository.signIn(newUser)

        withContext(Dispatchers.Main.immediate) {
            val lastUserId = appSettingsRepository.getLastUserIdValue()
            val newId = newUser.uid

            if (lastUserId != newId) {
                userRepository.signInNewUserWipe(context)
            }
            Qonversion.setProperty(QUserProperties.CustomUserId, newId)
            Qonversion.identify(newId)
        }
    }
}