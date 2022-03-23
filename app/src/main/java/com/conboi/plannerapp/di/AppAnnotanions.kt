package com.conboi.plannerapp.di

import javax.inject.Qualifier

/**
 * [AppApplicationScope] that name is used to avoid conflicts with 3rd party library "Qonversion"
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class AppApplicationScope

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IODispatcher

/**
 * [ActivitySharedPref] uses PreferenceManager.getDefaultSharedPreferences() only for operate the app language before performing attachBaseContext
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ActivitySharedPref