package com.conboi.features.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conboi.plannerapp.data.source.local.repo.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroViewModel
@Inject
constructor(
    private val appSettingsRepository: AppSettingsRepository,
) : ViewModel() {
    fun updateFirstLaunch(state: Boolean) =
        viewModelScope.launch {
            appSettingsRepository.updateFirstLaunch(state)
        }
}
