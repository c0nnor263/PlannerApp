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
import android.view.WindowInsets
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.ActivityMainBinding
import com.conboi.plannerapp.ui.bottomsheet.BottomNavigationFragment
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.shared.firebase.FirebaseUserLiveData
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import com.qonversion.android.sdk.Qonversion
import com.qonversion.android.sdk.QonversionError
import com.qonversion.android.sdk.QonversionPermissionsCallback
import com.qonversion.android.sdk.dto.QPermission
import com.qonversion.android.sdk.dto.products.QProductRenewState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController
    private lateinit var signInRequest: BeginSignInRequest
    lateinit var oneTapClient: SignInClient

    private val viewModel: MainActivityViewModel by viewModels()

    private var appVibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setOnExitAnimationListener { splashScreenView ->
            ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_Y,
                0f,
                splashScreenView.view.height.toFloat()
            ).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = resources.getInteger(R.integer.reply_motion_duration_medium).toLong()
                doOnEnd {
                    splashScreenView.remove()
                }
                start()
            }
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Init UI
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(this)
        setSupportActionBar(binding.bottomAppBar)

        binding.bottomAppBar.setNavigationOnClickListener {
            bottomAppBarNavigate()
        }
        setBottomAppBarForMain()

        initApp()
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        appNavigation(
            destination = destination,
            notifyIntent = arguments?.getBoolean(NOTIFY_INTENT) == true
        )
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (isHideInput(view, motionEvent)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.hide(WindowInsets.Type.ime())
                } else {
                    hideSoftInput(view?.windowToken)
                }
                view?.clearFocus()
            }
        }
        return super.dispatchTouchEvent(motionEvent)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        navController =
            (supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment).navController
        appNavigation(
            notifyIntent = intent?.getBooleanExtra(NOTIFY_INTENT, false) == true
        )
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.user != null) {
            viewModel.reloadUser()
            checkPermissions()
        }
        appNavigation(
            notifyIntent = intent?.getBooleanExtra(NOTIFY_INTENT, false) == true
        )
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

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment)
        navController = navHostFragment.navController
        navController.handleDeepLink(intent)
    }


    private fun initApp() {
        MobileAds.initialize(this)
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            SafetyNetAppCheckProviderFactory.getInstance()
        )
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(resources.getString(R.string.web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

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

        viewModel.checkNewFriends { result, error ->
            if (error == null) {
                result?.let {
                    for (newFriend in it) {
                        getSystemService(NotificationManager::class.java).sendNewFriendNotification(
                            this,
                            resources.getString(
                                R.string.notification_new_friend,
                                newFriend.user_name
                            ),
                            newFriend.user_id.toInt()
                        )
                    }
                }
            }
        }

        //Observers
        viewModel.completedTaskSize.observe(this) {
            if (it > 0) {
                if (navController.currentDestination?.id == R.id.mainFragment) {
                    binding.tvCountCompleted.visibility = View.VISIBLE
                }
                binding.tvCountCompleted.text =
                    resources.getString(R.string.count_of_completed, it)
            } else {
                binding.tvCountCompleted.visibility = View.GONE
            }
        }

        viewModel.syncState.observe(this) { syncState ->
            binding.ivSyncStatus.setImageResource(
                when (syncState) {
                    SynchronizationState.COMPLETE_SYNC -> R.drawable.ic_baseline_check_24
                    SynchronizationState.PENDING_SYNC -> R.drawable.ic_baseline_sync_24
                    SynchronizationState.ERROR_SYNC -> R.drawable.ic_baseline_sync_problem_24
                    else -> R.drawable.ic_baseline_sync_disabled_24
                }
            )
        }

        viewModel.taskSize.observe(this) {
            binding.tvCountTasks.text =
                resources.getString(
                    R.string.count_of_tasks,
                    it,
                    MAX_TASK_COUNT + if (viewModel.premiumState.value == true) MIDDLE_COUNT else 0
                )
        }

        viewModel.vibrationState.observe(this) {
            appVibrator =
                if (it) {
                    val am = getSystemService(AudioManager::class.java)
                    if (am.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?)?.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
        }
        // PrivateMode
        viewModel.privateState.observe(this) {
            viewModel.updateServerPrivateMode(it)
        }

        viewModel.authState.observe(this) {
            when (it) {
                FirebaseUserLiveData.AuthenticationState.UNAUTHENTICATED -> {
                    viewModel.saveLastUserID()
                    navController.popBackStackAllInstances(
                        navController.currentBackStackEntry?.destination?.id!!,
                        true
                    )
                    if (navController.currentDestination?.id != R.id.loginFragment) {
                        navController.navigate(R.id.loginFragment)
                    }
                }
                FirebaseUserLiveData.AuthenticationState.AUTHENTICATED -> viewModel.initUser()
                null -> {}
            }
        }

    }

    private var lastClickTime: Long = 0
    private fun bottomAppBarNavigate() {
        if (SystemClock.elapsedRealtime() - lastClickTime < 500) return
        lastClickTime = SystemClock.elapsedRealtime()
        val bottomNavDrawerFragment = BottomNavigationFragment()
        bottomNavDrawerFragment.show(supportFragmentManager, bottomNavDrawerFragment.tag)
    }

    private fun createChannel(channelId: String, channelName: String) {
        val notificationChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(true)
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(
            notificationChannel
        )
    }

    private fun appNavigation(
        destination: NavDestination? =
            (supportFragmentManager.findFragmentById(R.id.navigation_host) as NavHostFragment).navController.currentDestination,
        notifyIntent: Boolean
    ) {
        var delay: Long = 0
        if (notifyIntent) {
            delay = 500
        }
        Handler(Looper.getMainLooper()).postDelayed({
            when (destination?.id) {
                R.id.loginFragment -> setBottomAppBarForLogin()
                R.id.mainFragment -> setBottomAppBarForMain()
                R.id.taskDetailsFragment -> setBottomAppBarForTaskDetails()
                R.id.searchFragment -> setBottomAppBarForSearch()
                R.id.friendsFragment -> setBottomAppBarForFriends()
                R.id.friendDetailsFragment -> setBottomAppBarForFriendDetails()
                R.id.profileFragment -> setBottomAppBarForProfile()
                R.id.subscribeFragment -> setBottomAppBarForSubscribe()
            }
            delay = 0
        }, delay)
    }

    fun checkPermissions() {
        Qonversion.checkPermissions(object : QonversionPermissionsCallback {
            override fun onSuccess(permissions: Map<String, QPermission>) {
                val premiumPermission = permissions[PREMIUM_PERMISSION]

                if (premiumPermission != null && premiumPermission.isActive()) {
                    viewModel.setPremiumUI()

                    binding.tvCountTasks.text =
                        resources.getString(
                            R.string.count_of_tasks,
                            viewModel.taskSize.value,
                            MAX_TASK_COUNT + MIDDLE_COUNT
                        )

                    lifecycleScope.launch {
                        val resubscribeAlert = viewModel.getResubscribeAlert()
                        val premiumType = viewModel.getPremiumType()

                        when (premiumPermission.renewState) {
                            QProductRenewState.NonRenewable,
                            QProductRenewState.WillRenew -> {
                                if (resubscribeAlert) {
                                    viewModel.updateResubscribeAlert(true)
                                }
                            }
                            QProductRenewState.BillingIssue -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    resources.getString(R.string.update_payment),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            QProductRenewState.Canceled -> {
                                if (!resubscribeAlert) {
                                    val premiumTypeMessage =
                                        when (premiumType.name) {
                                            PremiumType.STANDARD.name -> return@launch
                                            PremiumType.MONTH.name -> "\"${resources.getString(R.string.month_subscription)}\""
                                            PremiumType.SIX_MONTH.name -> "\"${resources.getString(R.string.six_month_subscription)}\""
                                            PremiumType.YEAR.name -> "\"${resources.getString(R.string.year_subscription)}\""
                                            else -> return@launch
                                        }

                                    val messageString = resources.getString(
                                        R.string.you_have_active_subscription,
                                        premiumTypeMessage
                                    )

                                    subscriptionStillActiveDialog(messageString)
                                    viewModel.updateResubscribeAlert(true)
                                }
                            }
                            QProductRenewState.Unknown -> {}
                        }
                    }
                } else setNonPremiumUI()
            }

            override fun onError(error: QonversionError) {
                showErrorToast(this@MainActivity, Exception(error.additionalMessage))
            }
        })
    }

    // Set UI for specific fragment
    private fun setBottomAppBarForLogin() {
        hideFabAndAppBar()
        setColor(OTHER_COLOR)
    }

    private fun setBottomAppBarForMain() {
        checkPermissions()
        showSyncTotalContent()

        updateFabMainWithDrawable(R.drawable.add_anim)
        binding.bottomAppBar.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
        showFabAndAppBar()

        setColor(MAIN_TAG)
    }

    private fun setBottomAppBarForTaskDetails() = with(binding) {
        updateFabMainWithDrawable(R.drawable.ic_baseline_check_24)

        hideBottomAppBar()
        bottomAppBar.setFabAlignmentModeAndReplaceMenu(
            BottomAppBar.FAB_ALIGNMENT_MODE_END,
            R.menu.b_app_bar_empty_menu
        )
        setColor(MAIN_TAG)
    }

    private fun setBottomAppBarForSearch() {
        hideFabAndAppBar()
        binding.bottomAppBar.visibility = View.GONE
        setColor(OTHER_COLOR)
    }

    private fun setBottomAppBarForFriends() = with(binding) {
        checkPermissions()
        hideSyncTotalContent()

        updateFabMainWithDrawable(R.drawable.ic_baseline_person_search_24)
        bottomAppBar.setFabAlignmentModeAndReplaceMenu(
            BottomAppBar.FAB_ALIGNMENT_MODE_END,
            R.menu.b_app_bar_empty_menu
        )
        showFabAndAppBar()

        setColor(FRIENDS_TAG)
    }

    private fun setBottomAppBarForFriendDetails() {
        hideFabAndAppBar()
        setColor(FRIENDS_TAG)
    }

    private fun setBottomAppBarForProfile() = with(binding) {
        checkPermissions()
        hideSyncTotalContent()

        updateFabMainWithDrawable(R.drawable.ic_baseline_edit_24)
        bottomAppBar.setFabAlignmentModeAndReplaceMenu(
            BottomAppBar.FAB_ALIGNMENT_MODE_END,
            R.menu.b_app_bar_empty_menu
        )
        showFabAndAppBar()
        setColor(PROFILE_TAG)
    }

    private fun setBottomAppBarForSubscribe() = with(binding) {
        checkPermissions()
        hideSyncTotalContent()

        updateFabMainWithDrawable(R.drawable.ic_baseline_check_24)

        bottomAppBar.setFabAlignmentModeAndReplaceMenu(
            BottomAppBar.FAB_ALIGNMENT_MODE_END,
            R.menu.b_app_bar_empty_menu
        )
        hideBottomAppBar()

        setColor(FRIENDS_TAG)
    }


    // Theme color
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
        @ColorRes val bottomAppBarColor: Int
        @ColorRes val fabMainColor: Int
        when (code) {
            MAIN_TAG -> {
                fabMainColor = getColor(R.color.secondaryDarkColorWater)
                bottomAppBarColor = getColor(R.color.primaryDarkColorWater)
            }
            FRIENDS_TAG -> {
                fabMainColor = getColor(R.color.secondaryDarkColorFire)
                bottomAppBarColor = getColor(R.color.primaryDarkColorFire)
            }
            PROFILE_TAG -> {
                fabMainColor = getColor(R.color.secondaryDarkColorTree)
                bottomAppBarColor = getColor(R.color.secondaryDarkColorTree)
            }
            OTHER_COLOR or SETTINGS_TAG -> {
                fabMainColor = getColor(R.color.secondaryDarkColorAir)
                bottomAppBarColor = getColor(R.color.secondaryDarkColorAir)
            }
            else -> {
                fabMainColor = getColor(R.color.secondaryDarkColorWater)
                bottomAppBarColor = getColor(R.color.primaryDarkColorWater)
            }
        }
        animateColorThemeChanging(bottomAppBarColor, fabMainColor)
    }

    private fun settingColorLightTheme(code: Int) {
        @ColorRes val bottomAppBarColor: Int
        @ColorRes val fabMainColor: Int
        when (code) {
            MAIN_TAG -> {
                fabMainColor = getColor(R.color.secondaryColorWater)
                bottomAppBarColor = getColor(R.color.primaryColorWater)
            }
            FRIENDS_TAG -> {
                fabMainColor = getColor(R.color.secondaryColorFire)
                bottomAppBarColor = getColor(R.color.primaryColorFire)
            }
            PROFILE_TAG -> {
                fabMainColor = getColor(R.color.secondaryColorTree)
                bottomAppBarColor = getColor(R.color.primaryColorTree)
            }
            OTHER_COLOR or SETTINGS_TAG -> {
                fabMainColor = getColor(R.color.secondaryColorAir)
                bottomAppBarColor = getColor(R.color.primaryColorAir)
            }
            else -> {
                fabMainColor = getColor(R.color.secondaryColorWater)
                bottomAppBarColor = getColor(R.color.primaryColorWater)
            }
        }
        animateColorThemeChanging(bottomAppBarColor, fabMainColor)
    }

    private fun animateColorThemeChanging(bottomAppBarColor: Int, fabMainColor: Int) {
        val defaultColor = binding.bottomAppBar.backgroundTint?.defaultColor
        ValueAnimator.ofObject(
            ArgbEvaluator(),
            defaultColor,
            bottomAppBarColor
        ).apply {
            duration = resources.getInteger(R.integer.color_animation_duration_large).toLong()
            addUpdateListener { animator ->
                binding.bottomAppBar.background?.setTint(animator.animatedValue as Int)
            }
            start()
        }

        ValueAnimator.ofObject(
            ArgbEvaluator(),
            defaultColor,
            fabMainColor
        ).apply {
            duration =
                resources.getInteger(R.integer.color_animation_duration_large).toLong()
            addUpdateListener { animator ->
                binding.fabMain.background?.setTint(animator.animatedValue as Int)
            }
            start()
        }
    }

    fun vibrateDefaultAmplitude(times: Int, milliseconds: Long = 25) =
        repeat(times) {
            appVibrator?.vibrate(
                VibrationEffect.createOneShot(
                    milliseconds,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }

    //Hide soft keyboard
    private fun isHideInput(view: View?, motionEvent: MotionEvent): Boolean {
        if (view != null && view is EditText) {
            view.getLocationInWindow(intArrayOf(0, 0))
            val left = 0
            val top = 0
            val bottom = top + view.getHeight()
            val right = left + view.getWidth()
            return !(motionEvent.x > left && motionEvent.x < right && motionEvent.y > top && motionEvent.y < bottom)
        }
        return false
    }

    private fun hideSoftInput(token: IBinder?) {
        if (token != null) {
            val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }


    private fun showSyncTotalContent() = with(binding) {
        tvCountTasks.visibility = View.VISIBLE
        tvCountCompleted.visibility = View.VISIBLE
        ivSyncStatus.visibility = View.VISIBLE
    }

    private fun hideSyncTotalContent() = with(binding) {
        tvCountTasks.visibility = View.GONE
        tvCountCompleted.visibility = View.GONE
        ivSyncStatus.visibility = View.GONE
    }


    // Show and hide app bar and fab
    private fun updateFabMainWithDrawable(@DrawableRes fabIconId: Int?) {
        binding.fabMain.hide()
        fabIconId?.let {
            binding.fabMain.setImageDrawable(
                ContextCompat.getDrawable(
                    binding.fabMain.context,
                    fabIconId
                )
            )
        }
        binding.fabMain.show()
    }

    fun showBottomAppBar() {
        binding.bottomAppBar.performShow()
    }

    private fun hideBottomAppBar() {
        binding.bottomAppBar.performHide()
    }

    private fun showFabAndAppBar() {
        binding.fabMain.show()
        binding.bottomAppBar.performShow()
    }

    private fun hideFabAndAppBar() {
        binding.fabMain.hide()
        binding.bottomAppBar.performHide()
    }

    // Premium
    private fun subscriptionStillActiveDialog(messageString: String): AlertDialog =
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.subscription_is_active))
            .setMessage(messageString)
            .setPositiveButton(resources.getString(R.string.subscribe)) { dialog, _ ->
                navController.navigate(R.id.subscribeFragment)
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.not_now)) { dialog, _ ->
                dialog.cancel()
            }
            .setCancelable(false)
            .show()

    private fun setNonPremiumUI() {
        viewModel.setNonPremiumUI()
        binding.tvCountTasks.text =
            resources.getString(
                R.string.count_of_tasks,
                viewModel.taskSize.value,
                MAX_TASK_COUNT
            )
    }

    fun loginInitApp() {
        viewModel.initUser(newSignIn = true)
    }

    fun beginGoogleSignIn(callback: (BeginSignInResult?, Exception?) -> Unit) {
        oneTapClient.beginSignIn(signInRequest)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    callback(task.result, null)
                } else {
                    callback(null, task.exception)
                }
            }
    }

    companion object QonversionSku {
        const val MONTH_PRODUCT = "max_month_subscription"
        const val SIX_MONTH_PRODUCT = "max_6month_subscription"
        const val YEAR_PRODUCT = "max_year_subscription"

        const val PREMIUM_PERMISSION = "premium_plannerApp"
    }
}