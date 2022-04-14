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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginEvent?>(null)
    val uiState = _uiState


    suspend fun isFirstLaunch() = appSettingsRepository.getIsFirstLaunchValue()

    suspend fun checkForNewUser(newUser: FirebaseUser?, context: Context) {
        withContext(Dispatchers.Main.immediate) {
            val lastUserId = appSettingsRepository.getLastUserIdValue()
            val newId = newUser!!.uid

            if (lastUserId != newId) {
                userRepository.signInNewUserWipe(context)
            }
            Qonversion.setProperty(QUserProperties.CustomUserId, newId)
            Qonversion.identify(newId)
        }
    }

    fun sendSuccessSignInEvent(user: FirebaseUser?, show: Boolean? = true) {
        _uiState.value = if (show == true) LoginEvent.Success(user!!) else null
    }

    fun sendErrorSignInEvent(exception: Exception?, show: Boolean? = true) {
        _uiState.value = if (show == true) LoginEvent.Error(exception!!) else null
    }


    sealed class LoginEvent {
        data class Success(val user: FirebaseUser) : LoginEvent()
        data class Error(val exception: Exception) : LoginEvent()
    }
}