package com.conboi.plannerapp.interfaces.dialog

import java.util.*

interface DeadlineDialogCallback {
    fun saveDeadline(calendar: Calendar, missed: Boolean)
    fun removeDeadline()
}