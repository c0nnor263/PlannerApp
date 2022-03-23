package com.conboi.plannerapp.ui.bottomsheet

import androidx.lifecycle.ViewModel
import com.conboi.plannerapp.data.source.local.repo.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BottomNavigationViewModel @Inject constructor(
    userRepository: UserRepository
) : ViewModel() {
    val user = userRepository.getUser()
    val isEmailVerified = user?.isEmailVerified == true
}