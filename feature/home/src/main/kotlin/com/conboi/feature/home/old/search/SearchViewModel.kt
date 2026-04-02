package com.conboi.features.main.search

//
// @HiltViewModel
// class SearchViewModel
//    @Inject
//    constructor(
//        savedState: SavedStateHandle,
//        taskRepository: TaskRepository
//    ) : ViewModel() {
//        val searchQuery = savedState.getLiveData(SEARCH_QUERY, "")
//
//        val sortedTasks = taskRepository.getSortedTasks(searchQuery)
//
//        companion object {
//            const val SEARCH_QUERY = "searchQuery"
//        }
//    }