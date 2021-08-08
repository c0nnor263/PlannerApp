package com.example.plannerapp.ui.fire

import androidx.lifecycle.*
import com.example.plannerapp.data.FriendType
import com.example.plannerapp.data.PreferencesManager
import com.example.plannerapp.data.TaskTypeDao
import com.example.plannerapp.network.FriendApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FriendApiStatus{LOADING,ERROR,DONE}

@HiltViewModel
class FireViewModel @Inject constructor(
    private val taskTypeDao: TaskTypeDao,
    private val preferencesManager: PreferencesManager,
    private val savedState: SavedStateHandle
): ViewModel(){

    private val _status = MutableLiveData<FriendApiStatus>()
    val status:LiveData<FriendApiStatus> = _status

    private val _friends = MutableLiveData<List<FriendType>>()
    val friend:LiveData<List<FriendType>> = _friends

    init {
        getFriendList()
    }

    private fun getFriendList(){
        viewModelScope.launch {
            _status.value = FriendApiStatus.LOADING
            try {
                _friends.value = FriendApi.retrofitService.getFriendList()
                _status.value = FriendApiStatus.DONE
            }catch (expection:Exception){
                _status.value = FriendApiStatus.ERROR
                _friends.value = listOf()
            }
        }
    }
}