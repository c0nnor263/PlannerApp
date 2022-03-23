package com.conboi.plannerapp.ui.main.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.conboi.plannerapp.data.source.local.repo.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedState: SavedStateHandle,
    taskRepository: TaskRepository,
) : ViewModel() {
    val searchQuery = savedState.getLiveData(SEARCH_QUERY, "")

    val sortedTasks = taskRepository.getSortedTasks(searchQuery)

    companion object {
        const val SEARCH_QUERY = "searchQuery"
    }
}