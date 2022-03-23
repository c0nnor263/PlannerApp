package com.conboi.plannerapp.interfaces

import android.view.View
import com.conboi.plannerapp.data.model.FriendType

interface FriendListInterface {
    fun onClick(friend: FriendType)
    fun onHold(view: View, id: String)
}