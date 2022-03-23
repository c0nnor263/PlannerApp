package com.conboi.plannerapp.di.modules

import android.app.Application
import androidx.room.Room
import com.conboi.plannerapp.data.dao.TaskDao
import com.conboi.plannerapp.data.model.TaskType.Companion.DATABASE_NAME
import com.conboi.plannerapp.data.source.local.database.TaskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(app: Application) = Room.databaseBuilder(
        app,
        TaskDatabase::class.java,
        DATABASE_NAME
    )
        .build()

    @Provides
    @Singleton
    fun provideTaskDao(db: TaskDatabase): TaskDao = db.getTaskDao()
}