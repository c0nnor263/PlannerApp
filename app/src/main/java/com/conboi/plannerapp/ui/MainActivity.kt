package com.conboi.plannerapp.ui


import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.conboi.plannerapp.R
import com.firebase.ui.auth.AuthUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*


@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.bottom_menu_water_fragment,
                R.id.bottom_menu_air_fragment,
                R.id.bottom_menu_fire_fragment,
                R.id.bottom_menu_tree_fragment
            )
        )
        setSupportActionBar(tool_bar)
        setupActionBarWithNavController(navController, appBarConfiguration)


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        with(bottomNav) {
            setupWithNavController(navController)
            setOnNavigationItemReselectedListener {}
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val waterBottom = bottomNav.menu.findItem(R.id.bottom_menu_water_fragment)
            if (waterBottom.isChecked) {
                waterBottom.setIcon(android.R.drawable.ic_menu_add)
                waterBottom.title = "Add task"
            } else {
                waterBottom.setIcon(R.drawable.ic_water_drops)
                waterBottom.title = "Main"
            }
            when (destination.id) {
                R.id.bottom_menu_water_fragment -> {
                    val typedValue = TypedValue()
                    setTheme(R.style.WaterTheme_PlannerApp)
                    theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
                    window.navigationBarColor = typedValue.data
                    bottomNav.visibility = View.VISIBLE
                }
                R.id.bottom_menu_air_fragment -> {
                    val typedValue = TypedValue()
                    setTheme(R.style.AirTheme_PlannerApp)
                    theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
                    window.navigationBarColor = typedValue.data
                    bottomNav.visibility = View.VISIBLE
                }
                R.id.bottom_menu_fire_fragment -> {
                    val typedValue = TypedValue()
                    setTheme(R.style.FireTheme_PlannerApp)
                    theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
                    window.navigationBarColor = typedValue.data
                    bottomNav.visibility = View.VISIBLE
                }
                R.id.bottom_menu_tree_fragment -> {
                    val typedValue = TypedValue()
                    setTheme(R.style.TreeTheme_PlannerApp)
                    theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
                    window.navigationBarColor = typedValue.data
                    bottomNav.visibility = View.VISIBLE
                }
                else -> bottomNav.visibility = View.INVISIBLE
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
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}