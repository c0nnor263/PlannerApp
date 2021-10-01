package com.conboi.plannerapp.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize

@Parcelize
@Keep
data class FriendType(
    val user_id: String = "",
    val user_request_code: Int = 0,
    val user_photo_url: String = "",
    val user_name: String = "",
    val user_email: String = "",
    val user_total_completed: Int = 0
):Parcelable