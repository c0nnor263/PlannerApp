package com.conboi.plannerapp.ui.friend.details

import androidx.lifecycle.*
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.data.source.remote.repo.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val friendRepository: FriendRepository
) : ViewModel() {
    private val _taskList = MutableLiveData<List<TaskType>>(ArrayList())
    val taskList: LiveData<List<TaskType>> = _taskList

    private val _individualPrivateMode = MutableLiveData<Boolean>()
    val individualPrivateMode: LiveData<Boolean> = _individualPrivateMode


    fun setIndividualPrivateMode(privateModeState: Boolean) {
        _individualPrivateMode.value = privateModeState
    }

    fun setTaskList(list: List<TaskType>) {
        _taskList.value = list
    }

    fun updatePrivateMode() {
        _individualPrivateMode.value = individualPrivateMode.value!!.not()
    }

    fun updatePrivateFriendServer(id: String, individualPrivateState: Boolean) =
        viewModelScope.launch {
            friendRepository.updatePrivateFriend(id, individualPrivateState)
        }

    fun downloadFriendTaskList(id: String, callback: (List<TaskType>?, Exception?) -> Unit) =
        viewModelScope.launch {
            friendRepository.downloadFriendTaskList(id, callback)
        }


    fun saveState() {
        savedStateHandle[INDIVIDUAL_PRIVATE_MODE] = individualPrivateMode.value
        savedStateHandle[TASK_LIST] = taskList.value
    }

    fun restoreState() {
        savedStateHandle.getLiveData<Boolean>(INDIVIDUAL_PRIVATE_MODE).value?.let {
            setIndividualPrivateMode(
                it
            )
        }
        savedStateHandle.getLiveData<List<TaskType>>(TASK_LIST).value?.let { setTaskList(it) }
    }

    companion object {
        private const val INDIVIDUAL_PRIVATE_MODE = "individualPrivateMode"
        private const val TASK_LIST = "taskList"
    }
}