package com.conboi.plannerapp.ui.fire

import androidx.lifecycle.*
import com.conboi.plannerapp.data.FriendType
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.TaskTypeDao
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

        }
    }
}