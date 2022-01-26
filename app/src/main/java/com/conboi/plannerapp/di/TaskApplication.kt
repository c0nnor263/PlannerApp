package com.conboi.plannerapp.di

import android.app.Application
import android.os.Handler
import android.os.Looper
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.conboi.plannerapp.utils.PROJECT_KEY
import com.qonversion.android.sdk.Qonversion
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class TaskApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        val imageLoader: ImageLoader by lazy {
            ImageLoader.Builder(applicationContext)
                .crossfade(true)
                .build()
        }
        return imageLoader
    }

    override fun onCreate() {
        super.onCreate()
        Handler(Looper.getMainLooper()).post {
            Qonversion.launch(this, PROJECT_KEY, false)
        }
    }
}