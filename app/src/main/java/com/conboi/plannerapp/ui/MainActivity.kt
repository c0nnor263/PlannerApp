package com.conboi.plannerapp.ui


import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.conboi.plannerapp.R
import com.google.android.material.bottomappbar.BottomAppBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*


@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main),
    NavController.OnDestinationChangedListener {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(this)
        //window.statusBarColor = Color.argb(40, 60, 50, 60)
        setupBottomAppBar()
    }

    private fun setupBottomAppBar() {
        setSupportActionBar(bottom_app_bar)
        setBottomAppBarForMain()
        bottom_app_bar.setNavigationOnClickListener {
            val bottomNavDrawerFragment = BottomNavigationDrawerFragment()
            bottomNavDrawerFragment.show(supportFragmentManager, bottomNavDrawerFragment.tag)
        }

    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        when (destination.id) {
            R.id.mainFragment -> {
                setBottomAppBarForMain()
            }
            R.id.friendsFragment -> {
                setBottomAppBarForFriends()
            }
            R.id.settingsFragment -> {
                setBottomAppBarForSettings()
            }
            R.id.profileSettingsFragment -> {
                setBottomAppBarForProfileSettings()
            }
            R.id.taskDetailsFragment -> {
                setBottomAppBarForTaskDetails()
            }
            R.id.searchFragment -> {
                setBottomAppBarForSearch()
            }
            R.id.friendDetailsFragment -> {
                setBottomAppBarForFriendDetails()
            }
        }
    }

    private fun setBottomAppBarForMain() {
        bottom_app_bar_count_of_tasks.visibility = View.VISIBLE
        bottom_floating_button.hide()
        bottom_floating_button.setImageDrawable(
            ContextCompat.getDrawable(
                bottom_floating_button.context,
                R.drawable.ic_baseline_add_24
            )
        )
        bottom_floating_button.show()
        bottom_app_bar.fabAlignmentMode =
            BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
        bottom_app_bar.performShow()
        setColor(0)
    }

    private fun setBottomAppBarForFriends() {
        bottom_floating_button.hide()
        bottom_floating_button.setImageDrawable(
            ContextCompat.getDrawable(
                bottom_floating_button.context,
                R.drawable.ic_baseline_add_24
            )
        )
        bottom_floating_button.show()

        bottom_app_bar.setFabAlignmentModeAndReplaceMenu(
            BottomAppBar.FAB_ALIGNMENT_MODE_END,
            R.menu.bottom_app_bar_friends_menu
        )
        bottom_app_bar.performShow()
        setColor(1)
    }

    private fun setBottomAppBarForSettings() {
        bottom_floating_button.hide()
        bottom_floating_button.setImageDrawable(
            ContextCompat.getDrawable(
                bottom_floating_button.context,
                R.drawable.ic_check_mark
            )
        )
        bottom_floating_button.show()

        bottom_app_bar.setFabAlignmentModeAndReplaceMenu(
            BottomAppBar.FAB_ALIGNMENT_MODE_END,
            R.menu.bottom_app_bar_settings_menu
        )
        bottom_app_bar.performShow()
        setColor(2)
    }

    private fun setBottomAppBarForProfileSettings() {
        bottom_floating_button.hide()
        bottom_floating_button.setImageDrawable(
            ContextCompat.getDrawable(
                bottom_floating_button.context,
                R.drawable.ic_baseline_edit_24
            )
        )
        bottom_floating_button.show()

        bottom_app_bar.setFabAlignmentModeAndReplaceMenu(
            BottomAppBar.FAB_ALIGNMENT_MODE_CENTER,
            R.menu.bottom_app_bar_empty_menu
        )
        bottom_app_bar.performHide()

        setColor(0)
    }

    private fun setBottomAppBarForTaskDetails() {
        bottom_floating_button.hide()
        bottom_floating_button.setImageDrawable(
            ContextCompat.getDrawable(
                bottom_floating_button.context,
                R.drawable.ic_check_mark
            )
        )
        bottom_floating_button.show()

        bottom_app_bar.setFabAlignmentModeAndReplaceMenu(
            BottomAppBar.FAB_ALIGNMENT_MODE_END,
            R.menu.bottom_app_bar_empty_menu
        )
        bottom_app_bar.performHide()
        setColor(0)
    }

    private fun setBottomAppBarForSearch() {
        bottom_floating_button.hide()
        bottom_app_bar.performHide()
    }

    private fun setBottomAppBarForFriendDetails() {
        bottom_floating_button.hide()
        bottom_app_bar.performHide()
        setColor(1)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        when (navController.currentDestination?.id) {
            R.id.mainFragment -> {
                setBottomAppBarForMain()
            }
            R.id.friendsFragment -> {
                setBottomAppBarForFriends()
            }
            R.id.settingsFragment -> {
                setBottomAppBarForSettings()
            }
            R.id.profileSettingsFragment -> {
                setBottomAppBarForProfileSettings()
            }
            R.id.taskDetailsFragment -> {
                setBottomAppBarForTaskDetails()
            }
            R.id.searchFragment -> {
                setBottomAppBarForSearch()
            }
            R.id.friendDetailsFragment -> {
                setBottomAppBarForSettings()
            }
        }
    }

    private fun setColor(codeColor: Int) {
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                when (codeColor) {
                    0 -> {
                        val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.primaryColorWater
                            )
                        )
                        bottomAppBarColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomAppBarColorAnimation.addUpdateListener { animator ->
                            bottom_app_bar.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomAppBarColorAnimation.start()

                        val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.secondaryColorWater
                            )
                        )
                        bottomFloatingButtonColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                            bottom_floating_button.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomFloatingButtonColorAnimation.start()
                    }
                    1 -> {
                        val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.primaryColorFire
                            )
                        )
                        bottomAppBarColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomAppBarColorAnimation.addUpdateListener { animator ->
                            bottom_app_bar.background.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomAppBarColorAnimation.start()

                        val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.secondaryColorFire
                            )
                        )
                        bottomFloatingButtonColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                            bottom_floating_button.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomFloatingButtonColorAnimation.start()


                    }
                    2 -> {
                        val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.primaryColorTree
                            )
                        )
                        bottomAppBarColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomAppBarColorAnimation.addUpdateListener { animator ->
                            bottom_app_bar.background.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomAppBarColorAnimation.start()

                        val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.secondaryColorTree
                            )
                        )
                        bottomFloatingButtonColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                            bottom_floating_button.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomFloatingButtonColorAnimation.start()
                    }
                    3 -> {
                        val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.primaryColorAir
                            )
                        )
                        bottomAppBarColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomAppBarColorAnimation.addUpdateListener { animator ->
                            bottom_app_bar.background.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomAppBarColorAnimation.start()

                        val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.secondaryColorAir
                            )
                        )
                        bottomFloatingButtonColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                            bottom_floating_button.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomFloatingButtonColorAnimation.start()
                    }
                    else -> {
                        val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.primaryColorWater
                            )
                        )
                        bottomAppBarColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomAppBarColorAnimation.addUpdateListener { animator ->
                            bottom_app_bar.background.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomAppBarColorAnimation.start()

                        val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.secondaryColorWater
                            )
                        )
                        bottomFloatingButtonColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                            bottom_floating_button.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomFloatingButtonColorAnimation.start()
                    }
                }
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                when (codeColor) {
                    0 -> {
                        val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.primaryDarkColorWater
                            )
                        )
                        bottomAppBarColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomAppBarColorAnimation.addUpdateListener { animator ->
                            bottom_app_bar.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomAppBarColorAnimation.start()

                        val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.secondaryDarkColorWater
                            )
                        )
                        bottomFloatingButtonColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                            bottom_floating_button.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomFloatingButtonColorAnimation.start()
                    }
                    1 -> {
                        val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.primaryDarkColorFire
                            )
                        )
                        bottomAppBarColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomAppBarColorAnimation.addUpdateListener { animator ->
                            bottom_app_bar.background.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomAppBarColorAnimation.start()

                        val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.secondaryDarkColorFire
                            )
                        )
                        bottomFloatingButtonColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                            bottom_floating_button.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomFloatingButtonColorAnimation.start()


                    }
                    2 -> {
                        val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.primaryDarkColorTree
                            )
                        )
                        bottomAppBarColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomAppBarColorAnimation.addUpdateListener { animator ->
                            bottom_app_bar.background.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomAppBarColorAnimation.start()

                        val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.secondaryDarkColorTree
                            )
                        )
                        bottomFloatingButtonColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                            bottom_floating_button.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomFloatingButtonColorAnimation.start()
                    }
                    3 -> {
                        val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.primaryDarkColorAir
                            )
                        )
                        bottomAppBarColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomAppBarColorAnimation.addUpdateListener { animator ->
                            bottom_app_bar.background.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomAppBarColorAnimation.start()

                        val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.secondaryDarkColorAir
                            )
                        )
                        bottomFloatingButtonColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                            bottom_floating_button.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomFloatingButtonColorAnimation.start()
                    }
                    else -> {
                        val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.primaryDarkColorWater
                            )
                        )
                        bottomAppBarColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomAppBarColorAnimation.addUpdateListener { animator ->
                            bottom_app_bar.background.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomAppBarColorAnimation.start()

                        val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            bottom_app_bar.backgroundTint?.defaultColor,
                            ContextCompat.getColor(
                                this,
                                R.color.secondaryDarkColorWater
                            )
                        )
                        bottomFloatingButtonColorAnimation.duration =
                            resources.getInteger(R.integer.color_animation_duration_large).toLong()
                        bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                            bottom_floating_button.background?.setTint(
                                animator.animatedValue as Int
                            )
                        }
                        bottomFloatingButtonColorAnimation.start()
                    }
                }
            }
        }
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (isHideInput(view, ev)) {
                hideSoftInput(view!!.windowToken)
                view.clearFocus()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    //Decide if you need to hide
    private fun isHideInput(v: View?, ev: MotionEvent): Boolean {
        if (v != null && v is EditText) {
            val l = intArrayOf(0, 0)
            v.getLocationInWindow(l)
            val left = l[0]
            val top = l[1]
            val bottom = top + v.getHeight()
            val right = left + v.getWidth()
            return !(ev.x > left && ev.x < right && ev.y > top && ev.y < bottom)
        }
        return false
    }

    //Hide soft keyboard
    private fun hideSoftInput(token: IBinder?) {
        if (token != null) {
            val manager: InputMethodManager =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}