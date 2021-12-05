package com.conboi.plannerapp.di

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TaskApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
         return ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .build()
    }
}