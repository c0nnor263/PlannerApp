package com.example.plannerapp.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TaskType::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskTypeDao

}