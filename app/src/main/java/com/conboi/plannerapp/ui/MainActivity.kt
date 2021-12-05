package com.conboi.plannerapp.ui

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.SynchronizationState
import com.conboi.plannerapp.data.dataStore
import com.conboi.plannerapp.databinding.ActivityMainBinding
import com.conboi.plannerapp.ui.bottomsheet.BottomNavigationFragment
import com.conboi.plannerapp.ui.main.MainFragment.Companion.KEY_USER_EMAIL
import com.conboi.plannerapp.ui.main.MainFragment.Companion.KEY_USER_EMAIL_CONFIRM
import com.conboi.plannerapp.ui.main.MainFragment.Companion.KEY_USER_ID
import com.conboi.plannerapp.ui.main.MainFragment.Companion.KEY_USER_NAME
import com.conboi.plannerapp.ui.main.MainFragment.Companion.KEY_USER_PHOTO_URL
import com.conboi.plannerapp.ui.main.MainFragment.Companion.KEY_USER_PRIVATE_MODE
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.myclass.FirebaseUserLiveData
import com.conboi.plannerapp.utils.workers.CloudSyncWorker
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

const val MAIN_TAG = 0
const val FRIENDS_TAG = 1
const val PROFILE_TAG = 2
const val SETTINGS_TAG = 3
const val OTHER_COLOR = 3

@AndroidEntryPoint
class MainActivity : AppCompatActivity(),
    NavController.OnDestinationChangedListener {
    private lateinit var navController: NavController
    private val sharedViewModel: SharedViewModel by viewModels()
    lateinit var binding: ActivityMainBinding

    var vb: Vibrator? = null
    val auth: FirebaseAuth = Firebase.auth
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onResume() {
        super.onResume()
        if (Firebase.auth.currentUser != null) {
            Firebase.auth.currentUser!!.reload()
        }
        restoreResumeInstanceState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Create your custom animation.
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_Y,
                0f,
                splashScreenView.view.height.toFloat()
            )
            slideUp.interpolator = AccelerateDecelerateInterpolator()
            slideUp.duration = resources.getInteger(R.integer.reply_motion_duration_medium).toLong()
            slideUp.doOnEnd { splashScreenView.remove() }
            slideUp.start()
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MobileAds.initialize(this)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(this)
        initApp()

        CoroutineScope(SupervisorJob()).launch {
            Firebase.auth.currentUser?.apply {
                val userInfo: MutableMap<String, Any> = HashMap()
                userInfo[KEY_USER_ID] = uid
                userInfo[KEY_USER_EMAIL] = email.toString()
                userInfo[KEY_USER_NAME] = displayName ?: email.toString()
                userInfo[KEY_USER_PHOTO_URL] = photoUrl.toString()
                userInfo[KEY_USER_EMAIL_CONFIRM] = isEmailVerified
                userInfo[KEY_USER_PRIVATE_MODE] =
                    dataStore.data.first()[PreferencesManager.PreferencesKeys.PRIVATE_MODE]
                        ?: false
                db.document("Users/${auth.currentUser!!.uid}").set(userInfo, SetOptions.merge())
            }
            updateLocale(
                this@MainActivity,
                Locale(
                    dataStore.data.first()[PreferencesManager.PreferencesKeys.LANGUAGE_STATE]
                        ?: Locale.getDefault().language
                )
            )
        }

        WorkManager.getInstance(this@MainActivity).enqueueUniquePeriodicWork(
            BACKGROUND_CLOUD_WORKER,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CloudSyncWorker>(
                8, TimeUnit.HOURS, 30,
                TimeUnit.MINUTES
            ).build()
        )

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
                    SynchronizationState.ERROR_SYNC -> {
                        binding.bottomAppBarSyncStatus.setImageResource(R.drawable.ic_baseline_sync_problem_24)
                    }
                    else -> {
                        binding.bottomAppBarSyncStatus.setImageResource(R.drawable.ic_baseline_sync_disabled_24)
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
        createChannel(
            getString(R.string.deadline_notification_channel_id),
            getString(R.string.deadline_notification_channel_name)
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
        setBottomAppBarForMain()
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        var delay: Long = 0
        if (arguments?.getBoolean(NOTIFY_INTENT) == true) {
            delay = 500
        }
        Handler().postDelayed({
            when (destination.id) {
                R.id.mainFragment -> {
                    setBottomAppBarForMain()
                }
                R.id.taskDetailsFragment -> {
                    setBottomAppBarForTaskDetails()
                }
                R.id.searchFragment -> {
                    setBottomAppBarForSearch()
                }
                R.id.friendsFragment -> {
                    setBottomAppBarForFriends()
                }
                R.id.friendDetailsFragment -> {
                    setBottomAppBarForFriendDetails()
                }
                R.id.profileFragment -> {
                    setBottomAppBarForProfile()
                }
            }
        }, delay)
    }


    private fun setColor(codeColor: Int) {
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                lifecycleScope.launch { settingColorLightTheme(codeColor) }
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                lifecycleScope.launch { settingColorNightTheme(codeColor) }
            }
        }
    }

    private fun settingColorNightTheme(code: Int) {
        val bottomAppBarColor: Int
        val bottomFloatingButtonColor: Int
        when (code) {
            MAIN_TAG -> {
                sharedViewModel.updateColorPalette(
                    R.color.primaryDarkColorWater,
                    R.color.secondaryDarkColorWater,
                    R.color.secondaryColorWater,
                    R.color.secondaryDarkColorWater
                )
                bottomAppBarColor = getColor(R.color.primaryDarkColorWater)
                bottomFloatingButtonColor = getColor(R.color.secondaryDarkColorWater)
            }
            FRIENDS_TAG -> {
                sharedViewModel.updateColorPalette(
                    R.color.primaryDarkColorFire,
                    R.color.secondaryDarkColorFire,
                    R.color.secondaryColorFire,
                    R.color.secondaryDarkColorFire
                )
                bottomAppBarColor = getColor(R.color.primaryDarkColorFire)
                bottomFloatingButtonColor = getColor(R.color.secondaryDarkColorFire)
            }
            PROFILE_TAG -> {
                sharedViewModel.updateColorPalette(
                    R.color.primaryDarkColorTree,
                    R.color.secondaryDarkColorTree,
                    R.color.secondaryColorTree,
                    R.color.secondaryDarkColorTree
                )
                bottomAppBarColor = getColor(R.color.primaryDarkColorTree)
                bottomFloatingButtonColor = getColor(R.color.secondaryDarkColorTree)
            }
            OTHER_COLOR or SETTINGS_TAG -> {
                sharedViewModel.updateColorPalette(
                    R.color.primaryDarkColorAir,
                    R.color.secondaryDarkColorAir,
                    R.color.secondaryColorAir,
                    R.color.secondaryDarkColorAir
                )
                bottomAppBarColor = getColor(R.color.primaryDarkColorAir)
                bottomFloatingButtonColor = getColor(R.color.secondaryDarkColorAir)
            }
            else -> {
                sharedViewModel.updateColorPalette(
                    R.color.primaryColorWater,
                    R.color.primaryLightColorWater,
                    R.color.secondaryColorWater,
                    R.color.secondaryDarkColorWater
                )
                bottomAppBarColor = getColor(R.color.primaryDarkColorWater)
                bottomFloatingButtonColor = getColor(R.color.secondaryDarkColorWater)
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

    private fun settingColorLightTheme(code: Int) {
        val bottomAppBarColor: Int
        val bottomFloatingButtonColor: Int
        when (code) {
            MAIN_TAG -> {
                sharedViewModel.updateColorPalette(
                    R.color.primaryColorWater,
                    R.color.primaryLightColorWater,
                    R.color.secondaryColorWater,
                    R.color.secondaryDarkColorWater
                )
                bottomAppBarColor = getColor(R.color.primaryColorWater)
                bottomFloatingButtonColor = getColor(R.color.secondaryColorWater)
            }
            FRIENDS_TAG -> {
                sharedViewModel.updateColorPalette(
                    R.color.primaryColorFire,
                    R.color.primaryLightColorFire,
                    R.color.secondaryColorFire,
                    R.color.secondaryDarkColorFire
                )
                bottomAppBarColor = getColor(R.color.primaryColorFire)
                bottomFloatingButtonColor = getColor(R.color.secondaryColorFire)
            }
            PROFILE_TAG -> {
                sharedViewModel.updateColorPalette(
                    R.color.primaryColorTree,
                    R.color.primaryLightColorTree,
                    R.color.secondaryColorTree,
                    R.color.secondaryDarkColorTree
                )
                bottomAppBarColor = getColor(R.color.primaryColorTree)
                bottomFloatingButtonColor = getColor(R.color.secondaryColorTree)
            }
            OTHER_COLOR or SETTINGS_TAG -> {
                sharedViewModel.updateColorPalette(
                    R.color.primaryColorAir,
                    R.color.primaryLightColorAir,
                    R.color.secondaryColorAir,
                    R.color.secondaryDarkColorAir
                )
                bottomAppBarColor = getColor(R.color.primaryColorAir)
                bottomFloatingButtonColor = getColor(R.color.secondaryColorAir)
            }
            else -> {
                sharedViewModel.updateColorPalette(
                    R.color.primaryColorWater,
                    R.color.primaryLightColorWater,
                    R.color.secondaryColorWater,
                    R.color.secondaryDarkColorWater
                )
                bottomAppBarColor = getColor(R.color.primaryColorWater)
                bottomFloatingButtonColor = getColor(R.color.secondaryColorWater)
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
            setColor(MAIN_TAG)
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

            bottomAppBar.performHide()
            bottomAppBar.setFabAlignmentModeAndReplaceMenu(
                BottomAppBar.FAB_ALIGNMENT_MODE_END,
                R.menu.bottom_app_bar_empty_menu
            )
            setColor(MAIN_TAG)
        }
    }

    private fun setBottomAppBarForSearch() {
        binding.bottomFloatingButton.hide()
        binding.bottomAppBar.performHide()
        setColor(OTHER_COLOR)
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
            setColor(FRIENDS_TAG)
        }
    }

    private fun setBottomAppBarForFriendDetails() {
        binding.bottomFloatingButton.hide()
        binding.bottomAppBar.performHide()
        setColor(FRIENDS_TAG)
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
            setColor(PROFILE_TAG)
        }
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoreResumeInstanceState()
    }

    private fun restoreResumeInstanceState(){
        when ((supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment).navController.currentDestination?.id) {
            R.id.mainFragment -> {
                setBottomAppBarForMain()
            }
            R.id.taskDetailsFragment -> {
                setBottomAppBarForTaskDetails()
            }
            R.id.searchFragment -> {
                setBottomAppBarForSearch()
            }
            R.id.friendsFragment -> {
                setBottomAppBarForFriends()
            }
            R.id.friendDetailsFragment -> {
                setBottomAppBarForFriendDetails()
            }
            R.id.profileFragment -> {
                setBottomAppBarForProfile()
            }
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        (supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment).navController.handleDeepLink(
            intent
        )
    }
}