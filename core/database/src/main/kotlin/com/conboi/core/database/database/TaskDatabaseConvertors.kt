package com.conboi.core.database.database

import androidx.room.TypeConverter
import com.conboi.core.domain.old.enums.Priority
import com.conboi.core.domain.old.enums.RepeatMode

class TaskDatabaseConvertors {
    // Int -> RepeatMode
    @TypeConverter
    fun fromIntToRepeatMode(value: Int?): RepeatMode? {
        return value?.let {
            when (it) {
                0 -> RepeatMode.Once
                1 -> RepeatMode.Daily
                2 -> RepeatMode.Weekly
                else -> RepeatMode.Once
            }
        }
    }

    // RepeatMode -> Int
    @TypeConverter
    fun fromRepeatModeToInt(value: RepeatMode?): Int? {
        return value?.let {
            when (it) {
                RepeatMode.Once -> 0
                RepeatMode.Daily -> 1
                RepeatMode.Weekly -> 2
            }
        }
    }

    // Int -> Priority
    @TypeConverter
    fun fromIntToPriority(value: Int?): Priority? {
        return value?.let {
            when (it) {
                0 -> Priority.LOW
                1 -> Priority.NORMAL
                2 -> Priority.MAJOR
                3 -> Priority.IMPORTANT
                else -> Priority.NORMAL
            }
        }
    }

    // Priority -> Int
    @TypeConverter
    fun fromPriorityToInt(value: Priority?): Int? {
        return value?.let {
            when (it) {
                Priority.LOW -> 0
                Priority.NORMAL -> 1
                Priority.MAJOR -> 2
                Priority.IMPORTANT -> 3
            }
        }
    }
}