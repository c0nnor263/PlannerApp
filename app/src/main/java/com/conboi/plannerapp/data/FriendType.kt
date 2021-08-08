package com.example.plannerapp.data

import com.squareup.moshi.Json

data class FriendType(
    val idFriend: String,
    @Json(name = "name_friend") val nameFriend: String,
    @Json(name = "task_list_friend")val friendsTaskList: TaskType
)