package com.conboi.plannerapp.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.ActivitySplashBinding


class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val scaleX = ObjectAnimator.ofFloat(
            binding.appIcon,
            View.SCALE_X, 0.7f, 1f
        ).apply {
            duration = 400
        }
        val scaleY = ObjectAnimator.ofFloat(
            binding.appIcon,
            View.SCALE_Y, 0.7f, 1f
        ).apply {
            duration = 400
        }

        val translateY = ObjectAnimator.ofFloat(
            binding.root,
            View.TRANSLATION_Y,
            0f,
            binding.root.height.toFloat()
        ).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = resources.getInteger(R.integer.reply_motion_duration_medium).toLong()
        }


        AnimatorSet().apply {
            play(scaleX).with(scaleY).before(translateY)
            doOnEnd {
                navigateToMainActivity()
            }
            start()
        }

    }


    private fun navigateToMainActivity() {
        Intent(this, MainActivity::class.java).run {
            startActivity(this)
            finish()
        }
    }
}