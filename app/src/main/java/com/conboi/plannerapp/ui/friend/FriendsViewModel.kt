package com.conboi.plannerapp.ui.friend

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conboi.plannerapp.data.model.FriendType
import com.conboi.plannerapp.data.source.local.repo.AppSettingsRepository
import com.conboi.plannerapp.data.source.local.repo.UserRepository
import com.conboi.plannerapp.data.source.remote.repo.FriendRepository
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    userRepository: UserRepository,
    appSettingsRepository: AppSettingsRepository,
    private val friendRepository: FriendRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FriendsEvent?>(null)
    val uiState = _uiState

    val privateState = appSettingsRepository.getPrivateModeState()
    val premiumState = userRepository.getPremiumState()

    fun getFriendQuery() = friendRepository.getFriendQuery()

    fun getFriendList(callback: (List<DocumentSnapshot>?, Exception?) -> Unit) =
        viewModelScope.launch {
            friendRepository.getFriendList(callback)
        }

    fun checkEveryFriendInfo(list: List<DocumentSnapshot>, callback: (Any?, Exception?) -> Unit) =
        viewModelScope.launch {
            friendRepository.checkEveryFriendInfo(list, callback)
        }

    fun addFriend(id: String, callback: (Any?, Exception?) -> Unit) =
        viewModelScope.launch {
            friendRepository.addFriend(id, callback)
        }

    fun deleteFriend(id: String, callback: (Any?, Exception?) -> Unit) =
        viewModelScope.launch {
            friendRepository.deleteFriend(id, callback)
        }

    fun denyFriendRequest(id: String) =
        viewModelScope.launch {
            friendRepository.denyFriendRequest(id)
        }

    fun inviteFriend(userPrivateState: Boolean, id: String, callback: (Any?, Exception?) -> Unit) =
        viewModelScope.launch {
            friendRepository.inviteFriend(userPrivateState, id, callback)
        }


    fun sendRequestFriendEvent(id: String?, show: Boolean? = true) {
        _uiState.value = if (show == true) FriendsEvent.ShowRequestFriend(id!!) else null
    }

    fun sendFriendOptionsEvent(view: View?, id: String?, show: Boolean? = true) {
        _uiState.value = if (show == true) FriendsEvent.ShowFriendOptions(view!!, id!!) else null
    }

    fun sendInviteFriendEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) FriendsEvent.ShowInviteFriend else null
    }

    fun sendNavigateDetailsEvent(friend: FriendType?, show: Boolean? = true) {
        _uiState.value = if (show == true) FriendsEvent.NavigateDetails(friend!!) else null
    }

    fun sendCheckFriendListEvent(show: Boolean? = true) {
        _uiState.value = if (show == true) FriendsEvent.CheckFriendList else null
    }

    fun sendGetPremiumEvent(alreadyGot: Boolean = false) {
        _uiState.value = FriendsEvent.GetPremium(alreadyGot)
    }

    sealed class FriendsEvent {
        data class GetPremium(val alreadyGot: Boolean) : FriendsEvent()
        data class NavigateDetails(val friend: FriendType) : FriendsEvent()
        data class ShowRequestFriend(val id: String) : FriendsEvent()
        data class ShowFriendOptions(val view: View, val id: String) : FriendsEvent()
        object ShowInviteFriend : FriendsEvent()
        object CheckFriendList : FriendsEvent()

    }
}