package com.conboi.plannerapp.model

import android.os.Parcelable
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import kotlinx.parcelize.Parcelize

@Parcelize
data class FriendType(
    val user_id: String = "",
    val user_request_code: Int = 0,
    val user_photo_url: String = "",
    val user_name: String = "",
    val user_email: String = "",
    val user_count_completed: Int = 0,
    val user_friend_adding_time: Long = GLOBAL_START_DATE,
    val user_private_mode: Boolean = false
) : Parcelable