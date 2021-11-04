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
class FriendDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _bufferFriend = MutableLiveData(FriendType())
    val bufferFriend: LiveData<FriendType> = _bufferFriend

    private val _friendsTasks = MutableLiveData<List<TaskType>>(ArrayList())
    val friendsTasks: LiveData<List<TaskType>> = _friendsTasks


    fun setFriendsTasksList(list: List<TaskType>) {
        _friendsTasks.value = list
    }

    fun setBufferFriend(friend: FriendType) {
        _bufferFriend.value = friend
    }

    fun saveState() {
        savedStateHandle.apply {
            set(BUFFER_FRIEND, bufferFriend.value)
            set(FRIENDS_TASKS_LIST, friendsTasks.value)
        }
    }

    fun retrieveState() {
        savedStateHandle.apply {
            getLiveData<FriendType>(BUFFER_FRIEND).value?.let { setBufferFriend(it) }
            getLiveData<List<TaskType>>(FRIENDS_TASKS_LIST).value?.let { setFriendsTasksList(it) }
        }
    }

    companion object {
        private const val BUFFER_FRIEND = "bufferFriend"
        private const val FRIENDS_TASKS_LIST = "friendTasksList"
    }
}