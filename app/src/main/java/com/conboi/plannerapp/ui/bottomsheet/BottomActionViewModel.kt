package com.conboi.plannerapp.ui.bottomsheet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conboi.plannerapp.data.source.local.repo.TaskRepository
import com.conboi.plannerapp.di.IODispatcher
import com.conboi.plannerapp.utils.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BottomActionViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    val completedTasksSize = taskRepository.getCompletedTaskCount()
    val overcompletedTasksSize = taskRepository.getOvercompletedTaskCount()

    fun onSortOrderSelected(sortOrder: SortOrder) =
        viewModelScope.launch {
            taskRepository.onSortOrderSelected(sortOrder)
        }

    fun deleteCompletedTasks(context: Context) = viewModelScope.launch {
        withContext(ioDispatcher) {
            taskRepository.deleteCompletedTasks(context)
        }
    }

    fun deleteOvercompletedTasks(context: Context) = viewModelScope.launch {
        withContext(ioDispatcher) {
            taskRepository.deleteOvercompletedTasks(context)
        }
    }
}