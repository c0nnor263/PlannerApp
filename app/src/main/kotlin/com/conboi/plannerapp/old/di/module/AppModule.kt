package com.conboi.plannerapp.old.di.module

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.conboi.core.domain.di.ActivitySharedPref
import com.conboi.core.domain.di.AppApplicationScope
import com.conboi.core.domain.di.IODispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @AppApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())

    @IODispatcher
    @Provides
    @Singleton
    fun provideDispatcherIO(): CoroutineDispatcher = Dispatchers.IO

//    @Provides
//    @Singleton
//    fun provideDiffUtilCallbackTaskLiteType() = TaskTypeDiffCallback()

    @ActivitySharedPref
    @Provides
    @Singleton
    fun provideActivitySharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
}