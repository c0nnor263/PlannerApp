package com.conboi.plannerapp.interfaces

import android.view.View

interface ListInterface {
    fun onClick(view: View, id: Int)
    fun onHold()
}