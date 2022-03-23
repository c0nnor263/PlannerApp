package com.conboi.plannerapp.interfaces.dialog

import java.util.*

interface ReminderDialogCallback {
    fun saveReminder(calendar: Calendar)
    fun removeReminder()
}