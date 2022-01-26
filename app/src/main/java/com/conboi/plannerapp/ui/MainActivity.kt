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
import android.media.AudioManager
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.android.billingclient.api.*
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.PremiumType
import com.conboi.plannerapp.data.SynchronizationState
import com.conboi.plannerapp.data.dataStore
import com.conboi.plannerapp.databinding.ActivityMainBinding
import com.conboi.plannerapp.ui.bottomsheet.BottomNavigationFragment
import com.conboi.plannerapp.ui.main.MainFragment
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.myclass.FirebaseUserLiveData
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.qonversion.android.sdk.*
import com.qonversion.android.sdk.dto.QPermission
import com.qonversion.android.sdk.dto.products.QProduct
import com.qonversion.android.sdk.dto.products.QProductRenewState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.*


const val MAIN_TAG = 0
const val FRIENDS_TAG = 1
const val PROFILE_TAG = 2
const val SETTINGS_TAG = 3
const val OTHER_COLOR = 3

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(),
    NavController.OnDestinationChangedListener {
    companion object AppSku {
        const val MONTH_PRODUCT = "max_month_subscription"
        const val SIX_MONTH_PRODUCT = "max_6month_subscription"
        const val YEAR_PRODUCT = "max_year_subscription"

        const val PREMIUM_PERMISSION = "premium_plannerApp"
    }

    private lateinit var navController: NavController
    private val sharedViewModel: SharedViewModel by viewModels()
    lateinit var binding: ActivityMainBinding

    private var mLastClickTime: Long = 0
    var vb: Vibrator? = null
    val auth: FirebaseAuth = Firebase.auth
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    var bufferPremiumType: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            SafetyNetAppCheckProviderFactory.getInstance()
        )
        //Init user
        lifecycleScope.launch {
            Firebase.auth.currentUser?.apply {
                FirebaseFirestore.getInstance()
                    .collection("Users/${auth.currentUser!!.uid}/FriendList")
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            return@addSnapshotListener
                        }
                        for (dc in snapshots!!.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED &&
                                dc.document.getLong(MainFragment.KEY_USER_REQUEST)
                                    ?.toInt() == 2
                            ) {
                                if (dc.document.getBoolean("isShown") != true) {
                                    FirebaseFirestore.getInstance()
                                        .document(
                                            "Users/${auth.currentUser!!.uid}/FriendList/${
                                                dc.document.getString(
                                                    MainFragment.KEY_USER_ID
                                                )
                                            }"
                                        ).set(hashMapOf("isShown" to true), SetOptions.merge())
                                    getSystemService(NotificationManager::class.java).sendNewFriendNotification(
                                        this@MainActivity,
                                        resources.getString(
                                            R.string.notification_new_friend,
                                            dc.document[MainFragment.KEY_USER_NAME]
                                        ),
                                        dc.document[MainFragment.KEY_USER_ID].toString()
                                            .filter { it.isDigit() }
                                            .toInt() + snapshots.documents.size
                                    )
                                } else {
                                    return@addSnapshotListener
                                }
                            }
                        }
                    }
                val userInfo: MutableMap<String, Any> = HashMap()
                userInfo[MainFragment.KEY_USER_ID] = uid
                userInfo[MainFragment.KEY_USER_EMAIL] = email.toString()
                userInfo[MainFragment.KEY_USER_NAME] = displayName ?: email.toString()
                userInfo[MainFragment.KEY_USER_PHOTO_URL] = photoUrl.toString()
                userInfo[MainFragment.KEY_USER_EMAIL_CONFIRM] = isEmailVerified
                userInfo[MainFragment.KEY_USER_PRIVATE_MODE] =
                    dataStore.data.first()[PreferencesManager.PreferencesKeys.PRIVATE_MODE]
                        ?: false
                db.document("Users/${auth.currentUser!!.uid}").set(userInfo, SetOptions.merge())
            }

            bufferPremiumType =
                dataStore.data.first()[PreferencesManager.PreferencesKeys.PREMIUM_TYPE]
        }

        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_Y,
                0f,
                splashScreenView.view.height.toFloat()
            )
            slideUp.interpolator = AccelerateDecelerateInterpolator()
            slideUp.duration = resources.getInteger(R.integer.reply_motion_duration_medium).toLong()
            slideUp.doOnEnd {
                splashScreenView.remove()
            }
            slideUp.start()
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initApp()
    }

    private fun initApp() {
        Handler(Looper.getMainLooper()).post {
            MobileAds.initialize(this)
            createChannels()
        }

        //Init UI
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(this)




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
            vibrationModeState.observe(this@MainActivity) {
                val am = getSystemService(AudioManager::class.java)
                vb = if (it) {
                    if (am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as Vibrator?
                        } else {
                            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            allTasksSize.observe(this@MainActivity) {
                binding.bottomAppBarCountOfTasks.text =
                    resources.getString(
                        R.string.count_of_tasks,
                        it,
                        maxTasksCount + if (premiumState.value == true) MIDDLE_COUNT else 0
                    )
            }
            authenticationState.observe(this@MainActivity) { authenticationState ->
                if (authenticationState == FirebaseUserLiveData.AuthenticationState.UNAUTHENTICATED) {
                    navController.popBackStack()
                    navController.navigate(R.id.loginFragment)
                }
            }
        }
    }

    private fun createChannels() {
        createChannel(
            getString(R.string.reminder_notification_channel_id),
            getString(R.string.reminder_notification_channel_name)
        )
        createChannel(
            getString(R.string.deadline_notification_channel_id),
            getString(R.string.deadline_notification_channel_name)
        )
        createChannel(
            getString(R.string.friends_notification_channel_id),
            getString(R.string.friends_notification_channel_name)
        )
    }

    fun checkPermissions() {
        Qonversion.syncPurchases()
        Qonversion.checkPermissions(object : QonversionPermissionsCallback {
            override fun onSuccess(permissions: Map<String, QPermission>) {
                val premiumPermission = permissions[PREMIUM_PERMISSION]

                if (premiumPermission != null && premiumPermission.isActive()) {
                    sharedViewModel.updatePremium(true)
                    binding.bottomAppBarCountOfTasks.text =
                        resources.getString(
                            R.string.count_of_tasks,
                            sharedViewModel.allTasksSize.value!!,
                            sharedViewModel.maxTasksCount + MIDDLE_COUNT
                        )
                    sharedViewModel.updateSyncState(SynchronizationState.COMPLETE_SYNC)
                    when (premiumPermission.renewState) {
                        QProductRenewState.NonRenewable,
                        QProductRenewState.WillRenew -> {
                            lifecycleScope.launch {
                                val premiumType =
                                    dataStore.data.first()[PreferencesManager.PreferencesKeys.PREMIUM_TYPE]
                                val sharedPref =
                                    getSharedPreferences(APP_FILE, Context.MODE_PRIVATE)
                                if (sharedPref.getBoolean(RESUBSCRIBE_ALERT, false)) {
                                    sharedPref.edit()
                                        .putBoolean(RESUBSCRIBE_ALERT, true)
                                        .apply()
                                    bufferPremiumType = premiumType
                                }
                            }
                            // WillRenew is the state of an auto-renewable subscription
                            // NonRenewable is the state of consumable/non-consumable IAPs that could unlock lifetime access
                        }
                        QProductRenewState.BillingIssue -> {
                            Toast.makeText(
                                this@MainActivity,
                                resources.getString(R.string.update_payment),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        QProductRenewState.Canceled -> {
                            val sharedPref =
                                getSharedPreferences(APP_FILE, Context.MODE_PRIVATE) ?: return
                            if (!sharedPref.getBoolean(RESUBSCRIBE_ALERT, false)
                            ) {
                                lifecycleScope.launch {
                                    val premiumType =
                                        dataStore.data.first()[PreferencesManager.PreferencesKeys.PREMIUM_TYPE]

                                    if (premiumType!! == bufferPremiumType) {
                                        MaterialAlertDialogBuilder(this@MainActivity)
                                            .setTitle(resources.getString(R.string.subscription_is_active))
                                            .setMessage(
                                                resources.getString(
                                                    R.string.you_have_active_subscription,
                                                    if (premiumType != PremiumType.STANDARD.name) {
                                                        when (premiumType) {
                                                            PremiumType.MONTH.name -> {
                                                                "\"${resources.getString(R.string.month_subscription)}\""
                                                            }
                                                            PremiumType.SIX_MONTH.name -> {
                                                                "\"${resources.getString(R.string.six_month_subscription)}\""
                                                            }
                                                            PremiumType.YEAR.name -> {
                                                                "\"${resources.getString(R.string.year_subscription)}\""
                                                            }
                                                            else -> {}
                                                        }
                                                    } else {
                                                        ""
                                                    }
                                                )
                                            )
                                            .setPositiveButton(resources.getString(R.string.subscribe)) { dialog, _ ->
                                                val findNavController =
                                                    (supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment).findNavController()
                                                findNavController.navigate(R.id.subscribeFragment)
                                                dialog.dismiss()
                                            }
                                            .setNegativeButton(resources.getString(R.string.not_now)) { dialog, _ ->
                                                dialog.cancel()
                                            }
                                            .setCancelable(false)
                                            .show()
                                        sharedPref.edit()
                                            .putBoolean(RESUBSCRIBE_ALERT, true)
                                            .apply()
                                        bufferPremiumType = premiumType
                                    } else {
                                        bufferPremiumType = premiumType
                                    }
                                }

                            }

                            // The user has turned off auto-renewal for the subscription, but the subscription has not expired yet.
                            // Prompt the user to resubscribe with a special offer.
                        }
                        QProductRenewState.Unknown -> {
                        }
                    }
                } else {
                    binding.bottomAppBarCountOfTasks.text =
                        resources.getString(
                            R.string.count_of_tasks,
                            sharedViewModel.allTasksSize.value,
                            sharedViewModel.maxTasksCount
                        )
                    sharedViewModel.updatePremium(false)
                    sharedViewModel.updatePremiumType(PremiumType.STANDARD)
                    sharedViewModel.updateSyncState(SynchronizationState.DISABLED_SYNC)
                }
            }

            override fun onError(error: QonversionError) {
                Toast.makeText(this@MainActivity, error.additionalMessage, Toast.LENGTH_SHORT)
                    .show()
            }

        })
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
        Handler(Looper.getMainLooper()).postDelayed({
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
                R.id.subscribeFragment -> {
                    setBottomAppBarForSubscribe()
                }
            }
            delay = 0
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
                sharedViewModel.updateColor(
                    R.color.secondaryDarkColorWater
                )
                bottomAppBarColor = getColor(R.color.primaryDarkColorWater)
                bottomFloatingButtonColor = getColor(R.color.secondaryDarkColorWater)
            }
            FRIENDS_TAG -> {
                sharedViewModel.updateColor(
                    R.color.secondaryDarkColorFire,

                    )
                bottomAppBarColor = getColor(R.color.primaryDarkColorFire)
                bottomFloatingButtonColor = getColor(R.color.secondaryDarkColorFire)
            }
            PROFILE_TAG -> {
                sharedViewModel.updateColor(
                    R.color.secondaryDarkColorTree,

                    )
                bottomAppBarColor = getColor(R.color.primaryDarkColorTree)
                bottomFloatingButtonColor = getColor(R.color.secondaryDarkColorTree)
            }
            OTHER_COLOR or SETTINGS_TAG -> {
                sharedViewModel.updateColor(

                    R.color.secondaryDarkColorAir,

                    )
                bottomAppBarColor = getColor(R.color.primaryDarkColorAir)
                bottomFloatingButtonColor = getColor(R.color.secondaryDarkColorAir)
            }
            else -> {
                sharedViewModel.updateColor(

                    R.color.primaryLightColorWater,

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
                sharedViewModel.updateColor(

                    R.color.primaryLightColorWater,

                    )
                bottomAppBarColor = getColor(R.color.primaryColorWater)
                bottomFloatingButtonColor = getColor(R.color.secondaryColorWater)
            }
            FRIENDS_TAG -> {
                sharedViewModel.updateColor(
                    R.color.primaryLightColorFire,

                    )
                bottomAppBarColor = getColor(R.color.primaryColorFire)
                bottomFloatingButtonColor = getColor(R.color.secondaryColorFire)
            }
            PROFILE_TAG -> {
                sharedViewModel.updateColor(
                    R.color.primaryLightColorTree,

                    )
                bottomAppBarColor = getColor(R.color.primaryColorTree)
                bottomFloatingButtonColor = getColor(R.color.secondaryColorTree)
            }
            OTHER_COLOR or SETTINGS_TAG -> {
                sharedViewModel.updateColor(
                    R.color.primaryLightColorAir,

                    )
                bottomAppBarColor = getColor(R.color.primaryColorAir)
                bottomFloatingButtonColor = getColor(R.color.secondaryColorAir)
            }
            else -> {
                sharedViewModel.updateColor(
                    R.color.primaryLightColorWater,
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
            bottomAppBar.performShow()
            setColor(PROFILE_TAG)
        }
    }

    private fun setBottomAppBarForSubscribe() {
        binding.apply {
            bottomAppBarCountOfTasks.visibility = View.GONE
            bottomAppBarCountOfCompleted.visibility = View.GONE
            bottomAppBarSyncStatus.visibility = View.GONE

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

            setColor(FRIENDS_TAG)
        }
    }


    private fun restoreResumeInstanceState() {
        var delay: Long = 0
        if (intent?.getBooleanExtra(NOTIFY_INTENT, false) == true) {
            delay = 500
        }
        Handler(Looper.getMainLooper()).postDelayed({
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
                R.id.subscribeFragment -> {
                    setBottomAppBarForSubscribe()
                }
            }
            delay = 0
        }, delay)
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


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoreResumeInstanceState()
    }

    override fun onResume() {
        super.onResume()
        if (Firebase.auth.currentUser != null) {
            Firebase.auth.currentUser!!.reload()
        }
        restoreResumeInstanceState()
        Qonversion.products(callback = object : QonversionProductsCallback {
            override fun onSuccess(products: Map<String, QProduct>) {
                checkPermissions()
            }

            override fun onError(error: QonversionError) {
            }
        })
    }

    override fun attachBaseContext(base: Context) {
        val activityPref = PreferenceManager.getDefaultSharedPreferences(base)
        val lang = activityPref.getString(
            LANGUAGE,
            Locale.getDefault().language
        ) ?: Locale.getDefault().language

        val configuration = Configuration()
        val localeList = LocaleList(Locale(lang))
        LocaleList.setDefault(localeList)
        configuration.setLocales(localeList)

        val context = base.createConfigurationContext(configuration)
        super.attachBaseContext(context)

        SplitCompat.install(this)
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