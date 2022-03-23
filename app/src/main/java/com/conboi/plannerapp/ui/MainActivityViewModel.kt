package com.conboi.plannerapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.conboi.plannerapp.data.model.FriendType
import com.conboi.plannerapp.data.source.local.repo.AppSettingsRepository
import com.conboi.plannerapp.data.source.local.repo.TaskRepository
import com.conboi.plannerapp.data.source.local.repo.UserRepository
import com.conboi.plannerapp.data.source.remote.repo.FriendRepository
import com.conboi.plannerapp.utils.PremiumType
import com.conboi.plannerapp.utils.SynchronizationState
import com.conboi.plannerapp.utils.shared.firebase.FirebaseUserLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    taskRepository: TaskRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val userRepository: UserRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {
    val authState = FirebaseUserLiveData().map {
        if (it != null) {
            FirebaseUserLiveData.AuthenticationState.AUTHENTICATED
        } else {
            FirebaseUserLiveData.AuthenticationState.UNAUTHENTICATED
        }
    }

    val completedTaskSize = taskRepository.getCompletedTaskCount()
    val syncState = userRepository.getSyncState()
    val user = userRepository.getUser()
    val privateState = appSettingsRepository.getPrivateModeState()
    val taskSize = taskRepository.getTaskSize()
    val premiumState = userRepository.getPremiumState()
    val vibrationState = appSettingsRepository.getVibrationModeState()

    fun checkNewFriends(callback: (List<FriendType>?, Exception?) -> Unit) = viewModelScope.launch {
        friendRepository.checkNewFriends(callback)
    }

    suspend fun getResubscribeAlert() = appSettingsRepository.getIsResubscribeShowedValue()

    suspend fun getPremiumType() = userRepository.getPremiumTypeValue()

    fun setPremiumUI() = viewModelScope.launch {
        userRepository.updatePremium(true)
        userRepository.updateSyncState(SynchronizationState.COMPLETE_SYNC)
    }

    fun setNonPremiumUI() = viewModelScope.launch {
        userRepository.updatePremium(false)
        userRepository.updatePremiumType(PremiumType.STANDARD)
        userRepository.updateSyncState(SynchronizationState.DISABLED_SYNC)
    }


    fun updateServerPrivateMode(state: Boolean) = viewModelScope.launch {
        userRepository.updateServerPrivateMode(state)
    }

    fun updateResubscribeAlert(state: Boolean) =
        viewModelScope.launch { appSettingsRepository.updateResubscribeShowed(state) }

    fun saveLastUserID() = viewModelScope.launch {
        user?.uid?.let {
            appSettingsRepository.updateLastUserId(it)
        }
    }

    fun reloadUser() = userRepository.reloadUser()

    fun initUser(newSignIn: Boolean = false) = viewModelScope.launch {
        userRepository.initUser(newSignIn)
    }
}