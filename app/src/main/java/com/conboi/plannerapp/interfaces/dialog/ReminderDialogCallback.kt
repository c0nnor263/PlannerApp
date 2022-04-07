package com.conboi.plannerapp.interfaces.dialog

import com.conboi.plannerapp.utils.RepeatMode
import java.util.*

interface ReminderDialogCallback {
    fun saveReminder(calendar: Calendar,  bufferRepeatMode: RepeatMode)
    fun removeReminder()
}