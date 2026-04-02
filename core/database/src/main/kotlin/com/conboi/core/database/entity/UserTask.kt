package com.conboi.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.conboi.core.domain.GLOBAL_START_DATE
import com.conboi.core.domain.old.enums.Priority
import com.conboi.core.domain.old.enums.RepeatMode

// TODO: Divide this entity into multiple entities grouped by related fields
@Entity
data class UserTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.NORMAL,
    val createdTime: Long = System.currentTimeMillis(),
    val reminderTime: Long = GLOBAL_START_DATE,
    val deadlineTime: Long = GLOBAL_START_DATE,
    val completedTime: Long = GLOBAL_START_DATE,
    val lastMultipleCompleteTime: Long = GLOBAL_START_DATE,
    val reminderRepeatMode: RepeatMode = RepeatMode.Once,
    val isMissed: Boolean = false,
    val isCompleted: Boolean = false,
    val totalCompleted: Int = 0
)