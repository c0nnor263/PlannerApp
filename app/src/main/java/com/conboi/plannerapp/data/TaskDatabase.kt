package com.conboi.plannerapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.conboi.plannerapp.model.TaskType

@Database(
    version = 2,
    entities = [TaskType::class]
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun getTaskDao(): TaskDao
}