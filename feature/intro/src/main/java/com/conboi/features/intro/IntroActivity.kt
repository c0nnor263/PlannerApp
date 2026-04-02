package com.conboi.features.intro

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.conboi.core.domain.LANGUAGE
import com.conboi.core.ui.R
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroPageTransformerType
import com.google.android.play.core.splitcompat.SplitCompat
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class IntroActivity : AppIntro() {
    private val viewModel: IntroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntro()
    }

    private fun initIntro() {
        val listOfFragments =
            listOf(
                AppIntroCustomFragment.newInstance(
                    title = resources.getString(R.string.slide1_title),
                    imageDrawable = R.drawable.ic_logo,
                    description = resources.getString(R.string.slide1_desc),
                    backgroundColor = ContextCompat.getColor(this, R.color.secondaryDarkColorWater),
                ),
                AppIntroCustomFragment.newInstance(
                    title = resources.getString(R.string.slide2_title),
                    imageDrawable = R.drawable.intro_checking_crop_gif,
                    description = resources.getString(R.string.slide2_desc),
                    backgroundColor = ContextCompat.getColor(this, R.color.secondaryDarkColorWater),
                ),
                AppIntroCustomFragment.newInstance(
                    title = resources.getString(R.string.slide3_title),
                    imageDrawable = R.drawable.intro_typing_crop_gif,
                    description = resources.getString(R.string.slide3_desc),
                    backgroundColor = ContextCompat.getColor(this, R.color.primaryDarkColorFire),
                ),
                AppIntroCustomFragment.newInstance(
                    title = resources.getString(R.string.slide4_title),
                    imageDrawable = R.drawable.intro_adding_multiple_gif,
                    description = resources.getString(R.string.slide4_desc),
                    backgroundColor = ContextCompat.getColor(this, R.color.primaryDarkColorFire),
                ),
                AppIntroCustomFragment.newInstance(
                    title = resources.getString(R.string.slide5_title),
                    imageDrawable = R.drawable.intro_deleting_crop_gif,
                    description = resources.getString(R.string.slide5_desc),
                    backgroundColor = ContextCompat.getColor(this, R.color.secondaryDarkColorTree),
                ),
                AppIntroCustomFragment.newInstance(
                    title = resources.getString(R.string.delete_friend),
                    imageDrawable = R.drawable.intro_deleting_friend_crop_gif,
                    description = resources.getString(R.string.slide6_desc),
                    backgroundColor = ContextCompat.getColor(this, R.color.secondaryDarkColorTree),
                ),
                AppIntroCustomFragment.newInstance(
                    title = resources.getString(R.string.settings),
                    imageDrawable = R.drawable.intro_settings_gif,
                    description = resources.getString(R.string.slide7_desc),
                    backgroundColor = ContextCompat.getColor(this, R.color.secondaryDarkColorAir),
                ),
                AppIntroCustomFragment.newInstance(
                    title = resources.getString(R.string.slide8_title),
                    imageDrawable = R.drawable.intro_switch_tab,
                    description = resources.getString(R.string.slide8_desc),
                    backgroundColor = ContextCompat.getColor(this, R.color.secondaryDarkColorWater),
                ),
            )

        setImmersiveMode()
        isColorTransitionsEnabled = true
        isSystemBackButtonLocked = true

        setTransformer(
            AppIntroPageTransformerType.Parallax(
                titleParallaxFactor = 1.0,
                descriptionParallaxFactor = -1.0,
                imageParallaxFactor = 2.0,
            ),
        )

        repeat(listOfFragments.size) {
            addSlide(listOfFragments[it])
        }
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        viewModel.updateFirstLaunch(false)
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        viewModel.updateFirstLaunch(false)
        finish()
    }

    override fun attachBaseContext(base: Context) {
        val activityPref = PreferenceManager.getDefaultSharedPreferences(base)
        val lang =
            activityPref.getString(
                LANGUAGE,
                Locale.getDefault().language,
            ) ?: Locale.getDefault().language

        val configuration = Configuration()
        val localeList = LocaleList(Locale(lang))
        LocaleList.setDefault(localeList)
        configuration.setLocales(localeList)

        val context = base.createConfigurationContext(configuration)
        super.attachBaseContext(context)

        SplitCompat.install(this)
    }
}
