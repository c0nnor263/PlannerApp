package com.conboi.plannerapp.ui.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conboi.plannerapp.data.source.local.repo.AppSettingsRepository
import com.conboi.plannerapp.data.source.local.repo.UserRepository
import com.conboi.plannerapp.utils.PremiumType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscribeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {
    val premiumType = userRepository.getPremiumType()
    val selectedPremiumType = MutableLiveData<Int>()

    fun updateSelectedPremium(value: Int) {
        selectedPremiumType.value = value
    }

    fun updatePremium(state: Boolean) = viewModelScope.launch {
        userRepository.updatePremium(state)
    }

    fun setNewPremium(newPremiumType: PremiumType, userInfo: HashMap<String, Any>, importConfirmed:Boolean = false) =
        viewModelScope.launch {
            userRepository.updateUser(userInfo)
            userRepository.updatePremiumType(newPremiumType)
            appSettingsRepository.updateImportConfirmed(importConfirmed)
            appSettingsRepository.updateResubscribeShowed(false)
            updatePremium(true)
        }
}