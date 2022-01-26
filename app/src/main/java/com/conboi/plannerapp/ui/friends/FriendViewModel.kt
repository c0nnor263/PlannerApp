package com.conboi.plannerapp.ui.friends

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.conboi.plannerapp.model.FriendType
import com.conboi.plannerapp.model.TaskType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _bufferFriend = MutableLiveData(FriendType())
    val bufferFriend: LiveData<FriendType> = _bufferFriend

    private val _friendsTasks = MutableLiveData<List<TaskType>>(ArrayList())
    val friendsTasks: LiveData<List<TaskType>> = _friendsTasks

    private val _friendPrivateMode = MutableLiveData<Boolean>()
    val friendPrivateMode: LiveData<Boolean> = _friendPrivateMode


    fun setFriendsTasksList(list: List<TaskType>) {
        _friendsTasks.value = list
    }

    fun setBufferFriend(friend: FriendType) {
        _bufferFriend.value = friend
    }

    fun setFriendPrivateMode(privateMode: Boolean) {
        _friendPrivateMode.value = privateMode
    }

    fun updateFriendPrivateMode(privateMode: Boolean){
        _friendPrivateMode.value = privateMode
    }


    fun saveState() {
        savedStateHandle.apply {
            set(BUFFER_FRIEND, bufferFriend.value)
            set(FRIEND_TASK_LIST, friendsTasks.value)
            set(FRIEND_PRIVATE_MODE, friendPrivateMode.value)
        }
    }

    fun retrieveState() {
        savedStateHandle.apply {
            getLiveData<FriendType>(BUFFER_FRIEND).value?.let { setBufferFriend(it) }
            getLiveData<List<TaskType>>(FRIEND_TASK_LIST).value?.let { setFriendsTasksList(it) }
            getLiveData<Boolean>(FRIEND_PRIVATE_MODE).value?.let { setFriendPrivateMode(it) }
        }
    }

    companion object {
        private const val BUFFER_FRIEND = "bufferFriend"
        private const val FRIEND_TASK_LIST = "friendTaskList"
        private const val FRIEND_PRIVATE_MODE = "friendTaskList"
    }
}