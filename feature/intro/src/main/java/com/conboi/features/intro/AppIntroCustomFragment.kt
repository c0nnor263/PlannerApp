package com.conboi.features.intro

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import com.github.appintro.AppIntroBaseFragment
import com.github.appintro.AppIntroFragment
import com.github.appintro.model.SliderPage

/**
 * Class [AppIntroFragment] was overridden for ability to use GIFs 3rd party library [pl.droidsonroids.gif:android-gif-drawable:1.2.24]
 */
class AppIntroCustomFragment : AppIntroBaseFragment() {
    override val layoutId: Int get() = R.layout.intro_custom_layout

    companion object {
        @JvmOverloads
        @JvmStatic
        fun newInstance(
            title: CharSequence? = null,
            description: CharSequence? = null,
            @DrawableRes imageDrawable: Int = 0,
            @ColorInt backgroundColor: Int = 0,
            @ColorInt titleColor: Int = 0,
            @ColorInt descriptionColor: Int = 0,
            @FontRes titleTypefaceFontRes: Int = 0,
            @FontRes descriptionTypefaceFontRes: Int = 0,
            @DrawableRes backgroundDrawable: Int = 0,
        ): AppIntroCustomFragment {
            return newInstance(
                SliderPage(
                    title = title,
                    description = description,
                    imageDrawable = imageDrawable,
                    backgroundColor = backgroundColor,
                    titleColor = titleColor,
                    descriptionColor = descriptionColor,
                    titleTypefaceFontRes = titleTypefaceFontRes,
                    descriptionTypefaceFontRes = descriptionTypefaceFontRes,
                    backgroundDrawable = backgroundDrawable,
                ),
            )
        }

        @JvmStatic
        fun newInstance(sliderPage: SliderPage): AppIntroCustomFragment {
            val slide = AppIntroCustomFragment()
            slide.arguments = sliderPage.toBundle()
            return slide
        }
    }
}
