package com.conboi.plannerapp.old.di

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PlannerAppApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val imageLoader: ImageLoader by lazy {
            ImageLoader.Builder(this)
                .crossfade(true)
                .build()
        }
        return imageLoader
    }
}