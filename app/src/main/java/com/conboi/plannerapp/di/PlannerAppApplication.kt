package com.conboi.plannerapp.di

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.conboi.plannerapp.BuildConfig
import com.qonversion.android.sdk.Qonversion
import com.qonversion.android.sdk.QonversionConfig
import com.qonversion.android.sdk.dto.QLaunchMode
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PlannerAppApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        val qonversionConfig = QonversionConfig.Builder(
            this,
            BuildConfig.QonversionKEY,
            QLaunchMode.SubscriptionManagement
        ).build()
        Qonversion.initialize(qonversionConfig)
    }

    override fun newImageLoader(): ImageLoader {
        val imageLoader: ImageLoader by lazy {
            ImageLoader.Builder(this)
                .crossfade(true)
                .build()
        }
        return imageLoader
    }
}