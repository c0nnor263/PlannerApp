package com.conboi.plannerapp.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.SynchronizationState
import com.conboi.plannerapp.data.dataStore
import com.conboi.plannerapp.databinding.ActivityMainBinding
import com.conboi.plannerapp.ui.bottomsheet.BottomNavigationFragment
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.myclass.FirebaseUserLiveData
import com.conboi.plannerapp.utils.updateLocale
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import android.util.TypedValue





@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(),
    NavController.OnDestinationChangedListener {
    private lateinit var navController: NavController
    private val sharedViewModel: SharedViewModel by viewModels()
    lateinit var binding: ActivityMainBinding
    var vb: Vibrator? = null
    val auth: FirebaseAuth = Firebase.auth
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(this)
        initApp()

        CoroutineScope(SupervisorJob()).launch {
            updateLocale(
                this@MainActivity,
                Locale(
                    dataStore.data.first()[PreferencesManager.PreferencesKeys.LANGUAGE_STATE]
                        ?: Locale.getDefault().language
                )
            )
        }

        //Observers
        sharedViewModel.apply {
            allOnlyCompletedTasksSize.observe(this@MainActivity) {
                if (it > 0) {
                    if (navController.currentDestination?.id == R.id.mainFragment) {
                        binding.bottomAppBarCountOfCompleted.visibility = View.VISIBLE
                    }
                    binding.bottomAppBarCountOfCompleted.text =
                        resources.getString(R.string.count_of_completed, it)
                } else {
                    binding.bottomAppBarCountOfCompleted.visibility = View.GONE
                }
            }
            syncState.observe(this@MainActivity) { syncState ->
                when (syncState) {
                    SynchronizationState.COMPLETE_SYNC -> {
                        binding.bottomAppBarSyncStatus.setImageResource(R.drawable.ic_baseline_check_24)
                    }
                    SynchronizationState.PENDING_SYNC -> {
                        binding.bottomAppBarSyncStatus.setImageResource(R.drawable.ic_baseline_sync_24)
                    }
                    else -> {
                        binding.bottomAppBarSyncStatus.setImageResource(R.drawable.ic_baseline_sync_problem_24)
                    }
                }
            }
            allTasksSize.observe(this@MainActivity) {
                binding.bottomAppBarCountOfTasks.text =
                    resources.getString(
                        R.string.count_of_tasks,
                        it,
                        sharedViewModel.maxTasksCount
                    )
            }
            vibrationModeState.observe(this@MainActivity) {
                vb = if (it) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as Vibrator?
                    } else {
                        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
                    }
                } else {
                    null
                }
            }
            authenticationState.observe(this@MainActivity) { authenticationState ->
                if (authenticationState == FirebaseUserLiveData.AuthenticationState.UNAUTHENTICATED) {
                    navController.navigate(R.id.loginFragment)
                }
            }
        }

    }

    private fun createChannel(channelId: String, channelName: String) {
        val notificationChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(true)
        }

        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.enableVibration(true)

        getSystemService(NotificationManager::class.java).createNotificationChannel(
            notificationChannel
        )
    }

    private var mLastClickTime: Long = 0
    private fun initApp() {
        createChannel(
            getString(R.string.reminder_notification_channel_id),
            getString(R.string.reminder_notification_channel_name)
        )
        setSupportActionBar(binding.bottomAppBar)
        binding.bottomAppBar.setNavigationOnClickListener {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
                return@setNavigationOnClickListener
            }
            mLastClickTime = SystemClock.elapsedRealtime()
            val bottomNavDrawerFragment = BottomNavigationFragment()
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
            R.id.profileFragment -> {
                setBottomAppBarForProfile()
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
        binding.apply {
            bottomAppBarCountOfTasks.visibility = View.VISIBLE
            bottomAppBarCountOfCompleted.visibility = View.VISIBLE
            bottomAppBarSyncStatus.visibility = View.VISIBLE

            bottomFloatingButton.hide()
            bottomFloatingButton.setImageDrawable(
                ContextCompat.getDrawable(
                    bottomFloatingButton.context,
                    R.drawable.add_anim
                )
            )
            bottomFloatingButton.show()
            bottomAppBar.fabAlignmentMode =
                BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
            bottomAppBar.performShow()
            setColor(0)
        }
    }

    private fun setBottomAppBarForFriends() {
        binding.apply {
            bottomAppBarCountOfTasks.visibility = View.GONE
            bottomAppBarCountOfCompleted.visibility = View.GONE
            bottomAppBarSyncStatus.visibility = View.GONE

            bottomFloatingButton.hide()
            bottomFloatingButton.setImageDrawable(
                ContextCompat.getDrawable(
                    bottomFloatingButton.context,
                    R.drawable.ic_baseline_person_search_24
                )
            )
            bottomFloatingButton.show()

            bottomAppBar.setFabAlignmentModeAndReplaceMenu(
                BottomAppBar.FAB_ALIGNMENT_MODE_END,
                R.menu.bottom_app_bar_empty_menu
            )
            bottomAppBar.performShow()
            setColor(1)
        }
    }

    private fun setBottomAppBarForSettings() {
        binding.bottomFloatingButton.hide()
        binding.bottomAppBar.performHide()
    }

    private fun setBottomAppBarForProfile() {
        binding.apply {
            bottomAppBarCountOfTasks.visibility = View.GONE
            bottomAppBarCountOfCompleted.visibility = View.GONE
            bottomAppBarSyncStatus.visibility = View.GONE

            bottomFloatingButton.hide()
            bottomFloatingButton.setImageDrawable(
                ContextCompat.getDrawable(
                    bottomFloatingButton.context,
                    R.drawable.ic_baseline_edit_24
                )
            )
            bottomFloatingButton.show()

            bottomAppBar.setFabAlignmentModeAndReplaceMenu(
                BottomAppBar.FAB_ALIGNMENT_MODE_END,
                R.menu.bottom_app_bar_empty_menu
            )
            setColor(2)
        }
    }

    private fun setBottomAppBarForTaskDetails() {
        binding.apply {
            bottomFloatingButton.hide()
            bottomFloatingButton.setImageDrawable(
                ContextCompat.getDrawable(
                    bottomFloatingButton.context,
                    R.drawable.ic_baseline_check_24
                )
            )
            bottomFloatingButton.show()

            bottomAppBar.setFabAlignmentModeAndReplaceMenu(
                BottomAppBar.FAB_ALIGNMENT_MODE_END,
                R.menu.bottom_app_bar_empty_menu
            )
            bottomAppBar.performHide()
            setColor(0)
        }
    }

    private fun setBottomAppBarForSearch() {
        binding.bottomFloatingButton.hide()
        binding.bottomAppBar.performHide()
    }

    private fun setBottomAppBarForFriendDetails() {
        binding.bottomFloatingButton.hide()
        binding.bottomAppBar.performHide()
        setColor(1)
    }

    private fun setColor(codeColor: Int) {
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                settingColorLightTheme(codeColor)
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                settingColorNightTheme(codeColor)
            }
        }
    }

    private fun settingColorNightTheme(code: Int) {
        when (code) {
            0 -> {
                val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    binding.bottomAppBar.backgroundTint?.defaultColor,
                    ContextCompat.getColor(
                        this,
                        R.color.primaryDarkColorWater
                    )
                )
                bottomAppBarColorAnimation.duration =
                    resources.getInteger(R.integer.color_animation_duration_large).toLong()
                bottomAppBarColorAnimation.addUpdateListener { animator ->
                    binding.bottomAppBar.background?.setTint(
                        animator.animatedValue as Int
                    )
                }
                bottomAppBarColorAnimation.start()

                val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    binding.bottomAppBar.backgroundTint?.defaultColor,
                    ContextCompat.getColor(
                        this,
                        R.color.secondaryDarkColorWater
                    )
                )
                bottomFloatingButtonColorAnimation.duration =
                    resources.getInteger(R.integer.color_animation_duration_large).toLong()
                bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                    binding.bottomFloatingButton.background?.setTint(
                        animator.animatedValue as Int
                    )
                }
                bottomFloatingButtonColorAnimation.start()
            }
            1 -> {
                val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    binding.bottomAppBar.backgroundTint?.defaultColor,
                    ContextCompat.getColor(
                        this,
                        R.color.primaryDarkColorFire
                    )
                )
                bottomAppBarColorAnimation.duration =
                    resources.getInteger(R.integer.color_animation_duration_large).toLong()
                bottomAppBarColorAnimation.addUpdateListener { animator ->
                    binding.bottomAppBar.background.setTint(
                        animator.animatedValue as Int
                    )
                }
                bottomAppBarColorAnimation.start()

                val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    binding.bottomAppBar.backgroundTint?.defaultColor,
                    ContextCompat.getColor(
                        this,
                        R.color.secondaryDarkColorFire
                    )
                )
                bottomFloatingButtonColorAnimation.duration =
                    resources.getInteger(R.integer.color_animation_duration_large).toLong()
                bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                    binding.bottomFloatingButton.background?.setTint(
                        animator.animatedValue as Int
                    )
                }
                bottomFloatingButtonColorAnimation.start()


            }
            2 -> {
                val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    binding.bottomAppBar.backgroundTint?.defaultColor,
                    ContextCompat.getColor(
                        this,
                        R.color.primaryDarkColorTree
                    )
                )
                bottomAppBarColorAnimation.duration =
                    resources.getInteger(R.integer.color_animation_duration_large).toLong()
                bottomAppBarColorAnimation.addUpdateListener { animator ->
                    binding.bottomAppBar.background.setTint(
                        animator.animatedValue as Int
                    )
                }
                bottomAppBarColorAnimation.start()

                val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    binding.bottomAppBar.backgroundTint?.defaultColor,
                    ContextCompat.getColor(
                        this,
                        R.color.secondaryDarkColorTree
                    )
                )
                bottomFloatingButtonColorAnimation.duration =
                    resources.getInteger(R.integer.color_animation_duration_large).toLong()
                bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                    binding.bottomFloatingButton.background?.setTint(
                        animator.animatedValue as Int
                    )
                }
                bottomFloatingButtonColorAnimation.start()
            }
            3 -> {
                val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    binding.bottomAppBar.backgroundTint?.defaultColor,
                    ContextCompat.getColor(
                        this,
                        R.color.primaryDarkColorAir
                    )
                )
                bottomAppBarColorAnimation.duration =
                    resources.getInteger(R.integer.color_animation_duration_large).toLong()
                bottomAppBarColorAnimation.addUpdateListener { animator ->
                    binding.bottomAppBar.background.setTint(
                        animator.animatedValue as Int
                    )
                }
                bottomAppBarColorAnimation.start()

                val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    binding.bottomAppBar.backgroundTint?.defaultColor,
                    ContextCompat.getColor(
                        this,
                        R.color.secondaryDarkColorAir
                    )
                )
                bottomFloatingButtonColorAnimation.duration =
                    resources.getInteger(R.integer.color_animation_duration_large).toLong()
                bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                    binding.bottomFloatingButton.background?.setTint(
                        animator.animatedValue as Int
                    )
                }
                bottomFloatingButtonColorAnimation.start()
            }
            else -> {
                val bottomAppBarColorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    binding.bottomAppBar.backgroundTint?.defaultColor,
                    ContextCompat.getColor(
                        this,
                        R.color.primaryDarkColorWater
                    )
                )
                bottomAppBarColorAnimation.duration =
                    resources.getInteger(R.integer.color_animation_duration_large).toLong()
                bottomAppBarColorAnimation.addUpdateListener { animator ->
                    binding.bottomAppBar.background.setTint(
                        animator.animatedValue as Int
                    )
                }
                bottomAppBarColorAnimation.start()

                val bottomFloatingButtonColorAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    binding.bottomAppBar.backgroundTint?.defaultColor,
                    ContextCompat.getColor(
                        this,
                        R.color.secondaryDarkColorWater
                    )
                )
                bottomFloatingButtonColorAnimation.duration =
                    resources.getInteger(R.integer.color_animation_duration_large).toLong()
                bottomFloatingButtonColorAnimation.addUpdateListener { animator ->
                    binding.bottomFloatingButton.background?.setTint(
                        animator.animatedValue as Int
                    )
                }
                bottomFloatingButtonColorAnimation.start()
            }
        }
    }

    private fun settingColorLightTheme(code: Int) {
        val bottomAppBarColor: Int
        val bottomFloatingButtonColor: Int
        when (code) {
            0 -> {
                bottomAppBarColor = ContextCompat.getColor(this, R.color.primaryColorWater)
                bottomFloatingButtonColor = ContextCompat.getColor(this, R.color.secondaryColorWater)
            }
            1 -> {
                bottomAppBarColor = ContextCompat.getColor(this, R.color.primaryColorFire)
                bottomFloatingButtonColor = ContextCompat.getColor(this, R.color.secondaryColorFire)
            }
            2 -> {
                bottomAppBarColor = ContextCompat.getColor(this, R.color.primaryColorTree)
                bottomFloatingButtonColor = ContextCompat.getColor(this, R.color.secondaryColorTree)
            }
            3 -> {
                bottomAppBarColor = ContextCompat.getColor(this, R.color.primaryColorAir)
                bottomFloatingButtonColor = ContextCompat.getColor(this, R.color.secondaryColorAir)
            }
            else -> {
                bottomAppBarColor = ContextCompat.getColor(this, R.color.primaryColorWater)
                bottomFloatingButtonColor = ContextCompat.getColor(this, R.color.secondaryColorWater
                )
            }
        }

        ValueAnimator.ofObject(
            ArgbEvaluator(),
            binding.bottomAppBar.backgroundTint?.defaultColor,
            bottomAppBarColor
        ).apply {
            duration =
                resources.getInteger(R.integer.color_animation_duration_large).toLong()
            addUpdateListener { animator ->
                binding.bottomAppBar.background?.setTint(
                    animator.animatedValue as Int
                )
            }
            start()
        }

        ValueAnimator.ofObject(
            ArgbEvaluator(),
            binding.bottomAppBar.backgroundTint?.defaultColor,
            bottomFloatingButtonColor
        ).apply {
            duration =
                resources.getInteger(R.integer.color_animation_duration_large).toLong()
            addUpdateListener { animator ->
                binding.bottomFloatingButton.background?.setTint(
                    animator.animatedValue as Int
                )
            }
            start()
        }
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


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        when ((supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment).navController.currentDestination?.id) {
            R.id.mainFragment -> {
                setBottomAppBarForMain()
            }
            R.id.friendsFragment -> {
                setBottomAppBarForFriends()
            }
            R.id.settingsFragment -> {
                setBottomAppBarForSettings()
            }
            R.id.profileFragment -> {
                setBottomAppBarForProfile()
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

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}