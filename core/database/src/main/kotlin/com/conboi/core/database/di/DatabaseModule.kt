package com.conboi.core.database.di

import android.app.Application
import androidx.room.Room
import com.conboi.core.database.dao.UserTaskDao
import com.conboi.core.database.database.UserTaskDatabase
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
    fun provideDatabase(app: Application) =
        Room.databaseBuilder(
            app,
            UserTaskDatabase::class.java,
            UserTaskDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideTaskDao(db: UserTaskDatabase): UserTaskDao = db.getTaskDao()
}