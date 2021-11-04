package com.conboi.plannerapp.ui.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.TaskDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val totalCompleted = MutableLiveData(0)
    fun updateTotalCompleted(newTotal: Int) {
        totalCompleted.value = newTotal
    }
    suspend fun getTotalCompleted(): Int = preferencesManager.preferencesFlow.first().totalCompleted

    fun signOutDelete() =
        viewModelScope.launch {
            taskDao.deleteAllTasks()
            preferencesManager.updateTotalCompleted(0)
        }
}