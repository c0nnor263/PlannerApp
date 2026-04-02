package com.conboi.plannerapp.old.ui.profile

//
// @HiltViewModel
// class ProfileViewModel
//    @Inject
//    constructor(
//        taskRepository: TaskRepository,
//        private val userRepository: UserRepository
//    ) : ViewModel() {
//        val sortedTasks = taskRepository.getSortedTasks()
//
//        val totalCompleted = userRepository.getTotalCompleted()
//
//        val user = userRepository.getUser()
//
//        val premiumState = userRepository.getPremiumState()
//
//        fun verifyBeforeUpdateEmail(
//            newEmail: String,
//            callback: (Any?, Exception?) -> Unit
//        ) = viewModelScope.launch {
//            userRepository.verifyBeforeUpdateEmail(newEmail, callback)
//        }
//
//        fun updateUserName(newName: String) =
//            viewModelScope.launch {
//                userRepository.updateUserName(newName)
//            }
//
//        fun reloadUser() = userRepository.reloadUser()
//
//        fun updatePassword(
//            newPassword: String,
//            callback: (Any?, Exception?) -> Unit
//        ) = viewModelScope.launch {
//            userRepository.updatePassword(newPassword, callback)
//        }
//
//        fun reauthenticate(
//            currentPassword: String,
//            callback: (Any?, Exception?) -> Unit
//        ) = viewModelScope.launch {
//            userRepository.reauthenticate(currentPassword, callback)
//        }
//
//        fun resetPassword(callback: (Any?, Exception?) -> Unit) =
//            viewModelScope.launch {
//                userRepository.resetPassword(callback)
//            }
//
//        fun sendConfirmationEmail(callback: (Any?, Exception?) -> Unit) =
//            viewModelScope.launch {
//                userRepository.sendConfirmationEmail(callback)
//            }
//
//        fun signOut() {
//            userRepository.signOut()
//        }
//
//        fun signOutUploadTasks(
//            currentList: List<TaskType>,
//            callback: (Any?, Exception?) -> Unit
//        ) = viewModelScope.launch {
//            userRepository.signOutUploadTasks(currentList, callback)
//        }
//    }