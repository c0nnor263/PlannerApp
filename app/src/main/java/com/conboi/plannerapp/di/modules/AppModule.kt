package com.conboi.plannerapp.di.modules

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.conboi.plannerapp.adapter.TaskTypeDiffCallback
import com.conboi.plannerapp.di.ActivitySharedPref
import com.conboi.plannerapp.di.AppApplicationScope
import com.conboi.plannerapp.di.IODispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

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

    @Provides
    @Singleton
    fun provideDiffUtilCallbackTaskLiteType() = TaskTypeDiffCallback()

     @ActivitySharedPref
     @Provides
     @Singleton
     fun provideActivitySharedPreferences(@ApplicationContext context: Context): SharedPreferences =
         PreferenceManager.getDefaultSharedPreferences(context)
}