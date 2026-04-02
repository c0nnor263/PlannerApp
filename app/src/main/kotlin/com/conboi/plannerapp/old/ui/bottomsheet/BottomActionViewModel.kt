package com.conboi.plannerapp.old.ui.bottomsheet

// @HiltViewModel
// class BottomActionViewModel
//    @Inject
//    constructor(
//        private val taskRepository: TaskRepository,
//        @IODispatcher private val ioDispatcher: CoroutineDispatcher
//    ) : ViewModel() {
//        val completedTasksSize = taskRepository.getCompletedTaskCount()
//        val overcompletedTasksSize = taskRepository.getOvercompletedTaskCount()
//
//        fun onSortOrderSelected(sortOrder: SortOrder) =
//            viewModelScope.launch {
//                taskRepository.onSortOrderSelected(sortOrder)
//            }
//
//        fun deleteCompletedTasks(context: Context) =
//            viewModelScope.launch {
//                withContext(ioDispatcher) {
//                    taskRepository.deleteCompletedTasks(context)
//                }
//            }
//
//        fun deleteOvercompletedTasks(context: Context) =
//            viewModelScope.launch {
//                withContext(ioDispatcher) {
//                    taskRepository.deleteOvercompletedTasks(context)
//                }
//            }
//    }