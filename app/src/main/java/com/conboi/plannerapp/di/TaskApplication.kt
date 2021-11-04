package com.conboi.plannerapp.di

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TaskApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
         return ImageLoader.Builder(applicationContext)
            .crossfade(true)
            .build()

    }
}