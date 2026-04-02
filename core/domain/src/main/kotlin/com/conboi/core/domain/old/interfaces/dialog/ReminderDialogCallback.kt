package com.conboi.core.domain.old.interfaces.dialog

import com.conboi.core.domain.old.enums.RepeatMode
import java.util.Calendar

interface ReminderDialogCallback {
    fun saveReminder(
        calendar: Calendar,
        bufferRepeatMode: RepeatMode
    )

    fun removeReminder()
}