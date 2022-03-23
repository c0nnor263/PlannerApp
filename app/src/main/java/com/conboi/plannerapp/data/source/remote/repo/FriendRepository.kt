package com.conboi.plannerapp.data.source.remote.repo

import com.conboi.plannerapp.data.model.FriendType
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.data.source.local.repo.UserRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import javax.inject.Inject


@Module
@InstallIn(ActivityRetainedComponent::class)
class FriendRepository @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val userRepository: UserRepository
) {
    fun getFriendQuery(): Query = firebaseRepository.getFriendQuery()

    suspend fun getFriendList(callback: (List<DocumentSnapshot>?, Exception?) -> Unit) {
        val result = firebaseRepository.downloadFriendList()
        if (result.error == null) {
            callback(result.data, null)
        } else {
            callback(null, result.error)
        }
    }


    fun checkNewFriends(callback: (List<FriendType>?, Exception?) -> Unit) {
        val result = firebaseRepository.checkForNewFriends()
        if (result.error == null) {
            callback(result.data, null)
        } else {
            callback(null, result.error)
        }
    }

    suspend fun checkEveryFriendInfo(
        friendList: List<DocumentSnapshot>,
        callback: (Any?, Exception?) -> Unit,
    ) {
        val result = firebaseRepository.checkEveryFriendInfo(friendList)
        if (result.error == null) {
            callback(null, null)
        } else {
            callback(null, result.error)
        }
    }


    suspend fun addFriend(friendId: String, callback: (Any?, Exception?) -> Unit) {
        val result = firebaseRepository.acceptFriendRequest(friendId)
        if (result.error == null) {
            callback(null, null)
        } else {
            callback(null, result.error)
        }
    }

    suspend fun inviteFriend(
        userPrivateState: Boolean,
        id: String,
        callback: (Any?, Exception?) -> Unit
    ) {
        val totalCompleted = userRepository.getTotalCompletedValue()

        val result = firebaseRepository.inviteFriend(
            userPrivateState,
            totalCompleted,
            id
        )
        if (result.error == null) {
            callback(null, null)
        } else {
            callback(null, result.error)
        }
    }

    suspend fun denyFriendRequest(friendId: String) =
        firebaseRepository.denyFriendRequest(friendId)

    suspend fun deleteFriend(friendId: String, callback: (Any?, Exception?) -> Unit) {
        val result = firebaseRepository.deleteFriend(friendId)
        if (result.error == null) {
            callback(null, null)
        } else {
            callback(null, result.error)
        }
    }

    suspend fun updatePrivateFriend(id: String, privateState: Boolean) =
        firebaseRepository.updatePrivateFriend(id, privateState)

    suspend fun downloadFriendTaskList(
        friendId: String,
        callback: (List<TaskType>?, Exception?) -> Unit
    ) {
        val result = firebaseRepository.downloadFriendTasks(friendId)
        if (result.error == null) {
            callback(result.data, null)
        } else {
            callback(null, result.error)
        }
    }
}