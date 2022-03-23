package com.conboi.plannerapp.di

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.qonversion.android.sdk.Qonversion
import dagger.hilt.android.HiltAndroidApp

const val PROJECT_KEY = "ofRBlNjdwHFUFAYItO9sEVKOdaXcN0Aw"

@HiltAndroidApp
class TaskApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Qonversion.launch(this, PROJECT_KEY, false)
    }

    override fun newImageLoader(): ImageLoader {
        val imageLoader: ImageLoader by lazy {
            ImageLoader.Builder(applicationContext)
                .crossfade(true)
                .build()
        }
        return imageLoader
    }
}