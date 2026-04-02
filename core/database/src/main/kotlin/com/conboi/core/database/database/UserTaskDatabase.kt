package com.conboi.core.database.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.conboi.core.database.dao.UserTaskDao
import com.conboi.core.database.entity.UserTask

@Database(
    version = 1,
    entities = [UserTask::class],
    exportSchema = false
)
@TypeConverters(TaskDatabaseConvertors::class)
abstract class UserTaskDatabase : RoomDatabase() {
    abstract fun getTaskDao(): UserTaskDao

    companion object {
        const val DATABASE_NAME = "user_task_database"
    }
}