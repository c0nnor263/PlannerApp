package com.conboi.plannerapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.conboi.plannerapp.data.model.TaskType

@Database(entities = [TaskType::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

}