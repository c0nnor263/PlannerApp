package com.example.plannerapp.ui.tree

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.plannerapp.data.PreferencesManager
import com.example.plannerapp.data.TaskTypeDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class TreeViewModel @Inject constructor(
    private val taskTypeDao: TaskTypeDao,
    private val preferencesManager: PreferencesManager,
    private val savedState: SavedStateHandle
): ViewModel(){


}