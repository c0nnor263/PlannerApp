package com.conboi.plannerapp.data.source.local.database

import androidx.room.TypeConverter
import com.conboi.plannerapp.utils.Priority
import com.conboi.plannerapp.utils.RepeatMode

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
                0 -> Priority.LEISURELY
                1 -> Priority.DEFAULT
                2 -> Priority.ADVISABLE
                3 -> Priority.IMPORTANT
                else -> Priority.DEFAULT
            }
        }
    }

    // Priority -> Int
    @TypeConverter
    fun fromPriorityToInt(value: Priority?): Int? {
        return value?.let {
            when (it) {
                Priority.LEISURELY -> 0
                Priority.DEFAULT -> 1
                Priority.ADVISABLE -> 2
                Priority.IMPORTANT -> 3
            }
        }
    }
}