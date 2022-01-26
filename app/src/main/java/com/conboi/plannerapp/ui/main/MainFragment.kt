package com.conboi.plannerapp.ui.main

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.view.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.TaskAdapter
import com.conboi.plannerapp.data.PremiumType
import com.conboi.plannerapp.data.SynchronizationState
import com.conboi.plannerapp.databinding.FragmentMainBinding
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.bottomsheet.BottomActionsFragment
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.myclass.AlarmMethods
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.qonversion.android.sdk.Qonversion
import com.qonversion.android.sdk.QonversionError
import com.qonversion.android.sdk.QonversionPermissionsCallback
import com.qonversion.android.sdk.dto.QPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.DateFormat
import javax.inject.Inject


@AndroidEntryPoint
@ExperimentalCoroutinesApi
class MainFragment @Inject constructor() : BaseTabFragment(), TaskAdapter.OnTaskClickListener {
    @Inject
    lateinit var alarmMethods: AlarmMethods

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL_CONFIRM = "user_email_confirm"
        const val KEY_USER_PHOTO_URL = "user_photo_url"
        const val KEY_USER_REQUEST = "user_request_code"
        const val KEY_USER_FRIEND_ADDING_TIME = "user_friend_adding_time"
        const val KEY_USER_COUNT_COMPLETED_TASKS = "user_count_completed"
        const val KEY_USER_PRIVATE_MODE = "user_private_mode"
        const val KEY_USER_INDIVIDUAL_PRIVATE = "user_individual_private"
        const val KEY_USER_FRIEND_PRIVATE = "user_friend_private"
        const val KEY_USER_PREMIUM_TYPE = "user_premium_type"
        const val KEY_USER_LAST_SYNC = "user_last_sync"
    }

    private var _binding: FragmentMainBinding? = null
    val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val mAdapter: TaskAdapter = TaskAdapter(this)
    private var adView: AdView? = null

    //Firebase
    private var userInfoReference: DocumentReference? = null
    private var userTaskListReference: DocumentReference? = null
    private var userBackupTaskListReference: DocumentReference? = null

    var connection: Boolean? = null

    private var mLastClickTime: Long = 0
    private var countOfTasks: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.you_sure_quit))
                .setPositiveButton(resources.getString(R.string.exit)) { _, _ ->
                    requireActivity().finish()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
                .show()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).checkPermissions()
        initUser()
        //Animation
        postponeEnterTransition()
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        view.background.alpha = 35

        binding.lifecycleOwner = this
        (activity as MainActivity).let { mainActivity ->
            mainActivity.binding.bottomFloatingButton.apply {
                setOnClickListener {
                    addingTask()
                    (drawable as AnimatedVectorDrawable).start()
                }
                setOnLongClickListener {
                    mainActivity.vb?.vibrate(
                        VibrationEffect.createOneShot(
                            25,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                    addingMultipleTasks()
                    mainActivity.vb?.vibrate(
                        VibrationEffect.createOneShot(
                            25,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                    (drawable as AnimatedVectorDrawable).start()
                    (drawable as AnimatedVectorDrawable).start()
                    true
                }
            }
        }
        binding.rvTasks.apply {
            mAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    (binding.rvTasks.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        positionStart,
                        0
                    )
                }
            })
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            //Swipe delete task
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.ACTION_STATE_IDLE,
                ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSelectedChanged(
                    viewHolder: RecyclerView.ViewHolder?,
                    actionState: Int
                ) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        (activity as MainActivity).vb?.vibrate(
                            VibrationEffect.createOneShot(
                                50,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    }
                }

                override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.3f
                override fun getSwipeDirs(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    if ((viewHolder as TaskAdapter.ViewHolder).binding.title.isFocused) return ItemTouchHelper.ACTION_STATE_IDLE
                    return super.getSwipeDirs(recyclerView, viewHolder)
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    (activity as MainActivity).vb?.vibrate(
                        VibrationEffect.createOneShot(
                            25,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                    val task = mAdapter.currentList[viewHolder.bindingAdapterPosition]
                    sharedViewModel.swipeDeleteTask(task)
                    alarmMethods.cancelReminder(requireContext(), task.idTask)
                    alarmMethods.cancelDeadline(requireContext(), task.idTask)
                    val snackBar =
                        Snackbar.make(
                            binding.clSnack,
                            resources.getString(R.string.task_deleted),
                            Snackbar.LENGTH_LONG
                        )
                    snackBar.setAction(resources.getString(R.string.undo)) {
                        sharedViewModel.onUndoDeleteClick(task, requireContext())
                    }
                    snackBar.show()
                    (activity as MainActivity).binding.bottomAppBar.performShow()
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    val itemView: View = viewHolder.itemView
                    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        val p = Paint().also {
                            it.color = Color.RED
                        }
                        val d: Drawable =
                            ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24)!!
                        val icon = drawableToBitmap(d)

                        // Draw background
                        if (dX > 0.0) {
                            val rectF = RectF(
                                itemView.left.toFloat() - dX * 0.3F,
                                itemView.top.toFloat(),
                                itemView.right.toFloat() + dX * 0.3F,
                                itemView.bottom.toFloat()
                            )
                            c.drawRoundRect(
                                rectF,
                                40F,
                                40F,
                                p
                            )
                        } else {
                            val rectF = RectF(
                                itemView.left.toFloat() - dX * 0.3F,
                                itemView.top.toFloat(),
                                itemView.right.toFloat() + dX * 0.3F,
                                itemView.bottom.toFloat()
                            )
                            c.drawRoundRect(
                                rectF,
                                40F,
                                40F,
                                Paint().apply { color = Color.TRANSPARENT }
                            )
                        }
                        //Draw icon
                        if (dX > 10) {
                            val iconMarginLeft = (dX * 0.1F).coerceAtMost(80f).coerceAtLeast(10f)
                            c.drawBitmap(
                                icon!!,
                                iconMarginLeft,
                                itemView.top.toFloat() + (itemView.bottom.toFloat() - itemView.top.toFloat() - icon.height) / 2,
                                p
                            )
                        }
                    }
                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            }).attachToRecyclerView(this)
        }
        sharedViewModel.apply {
            privateModeState.observe(viewLifecycleOwner) {
                updateFirebasePrivateMode(it)
            }

            //LiveData size of tasks list
            allTasksSize.observe(this@MainFragment.viewLifecycleOwner) {
                if (it != 0) {
                    binding.tvNoTasks.visibility = View.GONE
                } else {
                    binding.tvNoTasks.visibility = View.VISIBLE
                }
                countOfTasks = it
            }

            //LiveData list of sorted tasks
            sortedTasks.observe(this@MainFragment.viewLifecycleOwner) {
                if (mAdapter.currentList.isNotEmpty()) {
                    updateFirebaseTotalCompletedTasks()
                }
                mAdapter.updatePremiumState(premiumState.value ?: false)
                mAdapter.submitList(it)
                (view.parent as? ViewGroup)?.doOnPreDraw {
                    startPostponedEnterTransition()

                }
            }

            //LiveData list of tasks
            allTasks.observe(this@MainFragment.viewLifecycleOwner) {
                if (mAdapter.currentList.isNotEmpty()) {
                    updateFirebaseCloudBackup(it)
                }
            }

            //LiveData premium state
            premiumState.observe(this@MainFragment.viewLifecycleOwner) {
                if (it) {
                    binding.adView.visibility = View.GONE
                    val scale = resources.displayMetrics.density
                    binding.rvTasks.updatePadding(top = (8 * scale + 0.5f).toInt())
                } else {
                    binding.adView.visibility = View.VISIBLE
                    val scale = resources.displayMetrics.density
                    binding.rvTasks.updatePadding(top = (55 * scale + 0.5f).toInt())
                    adView = binding.adView
                    adView?.loadAd(AdRequest.Builder().build())
                }
            }

            middleListTime.observe(this@MainFragment.viewLifecycleOwner) {
                mAdapter.updateMiddleTime(it)
            }

            var once = false
            rateUsCount.observe(this@MainFragment.viewLifecycleOwner) {
                if (it == 15) {
                    if (!once) {
                        promptRate()
                        once = true
                    }
                }
            }
        }
        setHasOptionsMenu(true)

    }

    private fun promptRate() {
        sharedViewModel.updateRateUs(16)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.rate_us))
            .setMessage(resources.getString(R.string.rate_us_message))
            .setPositiveButton(resources.getString(R.string.rate)) { dialog, _ ->
                val manager = ReviewManagerFactory.create(requireContext())
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(activity as MainActivity, reviewInfo)
                        flow.addOnCompleteListener {
                            sharedViewModel.updateRateUs(16)
                            dialog.dismiss()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            task.exception.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        val uri = Uri.parse("market://details?id=" + requireContext().packageName)
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        dialog.dismiss()
                    }
                }
            }
            .setNeutralButton(resources.getString(R.string.not_now)) { dialog, _ ->
                sharedViewModel.updateRateUs(0)
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.never)) { dialog, _ ->
                sharedViewModel.updateRateUs(16)
                dialog.cancel()
            }
            .show()
    }

    //Firebase
    private fun initUser() {
        (activity as MainActivity).let { mainActivity ->
            val connectedRef = Firebase.database.getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    connection = connected
                    if (mAdapter.currentList.isNotEmpty() && mainActivity.auth.currentUser != null) {
                        updateFirebaseCloudBackup(mAdapter.currentList)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
            val sharedPref =
                requireContext().getSharedPreferences(APP_FILE, Context.MODE_PRIVATE) ?: return
            if (mainActivity.auth.currentUser != null) {
                if (sharedViewModel.totalCompleted.value == 0) {
                    getTotalPremiumType()
                }
                mainActivity.auth.currentUser?.reload()
                if (!sharedPref.getBoolean(
                        EMAIL_CONFIRM,
                        false
                    )
                ) {
                    if (mainActivity.auth.currentUser?.isEmailVerified == false) {
                        sharedViewModel.updateSyncState(
                            SynchronizationState.DISABLED_SYNC
                        )
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(resources.getString(R.string.email_not_confirmed))
                            .setMessage(resources.getString(R.string.you_confirm_email))
                            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                                findNavController().navigate(R.id.profileFragment)
                                dialog.dismiss()
                            }
                            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->

                                dialog.cancel()
                            }
                            .show()
                    } else {
                        sharedViewModel.updateSyncState(
                            SynchronizationState.COMPLETE_SYNC
                        )

                    }
                    sharedPref.edit().putBoolean(EMAIL_CONFIRM, true).apply()

                }
                userInfoReference =
                    mainActivity.db.document("Users/${mainActivity.auth.currentUser?.uid}")
                userTaskListReference =
                    mainActivity.db.document("Users/${mainActivity.auth.currentUser?.uid}/TaskList/Tasks")
                userBackupTaskListReference =
                    mainActivity.db.document("Users/${mainActivity.auth.currentUser?.uid}/TaskList/BackupTasks")
                if (!(sharedPref.getBoolean(IMPORT_CONFIRM, false))) {
                    getTotalPremiumType()
                    (activity as MainActivity).binding.bottomFloatingButton.isEnabled = false
                    (activity as MainActivity).binding.bottomAppBar.isEnabled = false
                    importFirebaseTasks(sharedPref)
                }
            }
        }
    }

    private fun getTotalPremiumType() {
        //Get total completed and premium
        userInfoReference?.get()?.addOnCompleteListener { snapshot ->
            if (snapshot.isSuccessful) {
                sharedViewModel.updateTotalCompleted(
                    snapshot.result.getLong(KEY_USER_COUNT_COMPLETED_TASKS)
                        ?.toInt() ?: 0
                )
                if (sharedViewModel.premiumState.value == false) {
                    sharedViewModel.updatePremiumType(
                        when (snapshot.result.getString(KEY_USER_PREMIUM_TYPE)) {
                            PremiumType.MONTH.name -> {
                                PremiumType.MONTH
                            }
                            PremiumType.SIX_MONTH.name -> {
                                PremiumType.SIX_MONTH
                            }
                            PremiumType.YEAR.name -> {
                                PremiumType.YEAR
                            }
                            else -> {
                                return@addOnCompleteListener
                            }
                        }
                    )
                }
            }
        }
    }

    private fun importFirebaseTasks(sharedPref: SharedPreferences) {
        Qonversion.checkPermissions(object : QonversionPermissionsCallback {
            override fun onSuccess(permissions: Map<String, QPermission>) {
                val premiumPermission = permissions[MainActivity.PREMIUM_PERMISSION]
                promptSync(premiumPermission != null && premiumPermission.isActive(), sharedPref)
            }

            override fun onError(error: QonversionError) {
                Toast.makeText(requireContext(), error.additionalMessage, Toast.LENGTH_SHORT).show()
                (activity as MainActivity).binding.bottomFloatingButton.isEnabled = true
                (activity as MainActivity).binding.bottomAppBar.isEnabled = true
            }
        })

    }

    fun promptSync(premium: Boolean, sharedPref: SharedPreferences) {
        lifecycleScope.launch {
            val isBackupDownloaded = sharedPref.getBoolean(
                IMPORT_DOWNLOADED, false
            )
            userInfoReference?.get()?.addOnCompleteListener {
                if (it.isSuccessful) {
                    val lastSyncTime = it.result.getLong(KEY_USER_LAST_SYNC) ?: GLOBAL_START_DATE
                    val lastSyncString =
                        DateFormat.getDateTimeInstance(
                            DateFormat.DEFAULT,
                            DateFormat.DEFAULT,
                            resources.configuration.locales[0]
                        ).format(lastSyncTime)
                    //Get tasks
                    userBackupTaskListReference?.get()
                        ?.addOnCompleteListener { taskCollection ->
                            if (taskCollection.isSuccessful) {
                                if (taskCollection.result.exists() && !isBackupDownloaded) {
                                    val alertDialog = MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(resources.getString(R.string.warning))
                                        .setCancelable(false)

                                    if (premium) {
                                        alertDialog.setMessage(
                                            resources.getString(
                                                R.string.backup_tasks_message,
                                                lastSyncString
                                            )
                                        )
                                        alertDialog.setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                                            downloadFirebaseList(sharedPref)
                                            dialog.dismiss()
                                        }
                                        alertDialog.setNegativeButton(resources.getString(R.string.deny)) { dialog, _ ->
                                            (activity as MainActivity).binding.bottomFloatingButton.isEnabled =
                                                true
                                            (activity as MainActivity).binding.bottomAppBar.isEnabled =
                                                true
                                            with(sharedPref.edit()) {
                                                putBoolean(IMPORT_CONFIRM, true)
                                                apply()
                                            }
                                            dialog.dismiss()
                                        }
                                    } else {
                                        alertDialog.setMessage(
                                            resources.getString(
                                                R.string.backup_tasks_message_no_prem,
                                                lastSyncString
                                            )
                                        )
                                        alertDialog.setPositiveButton(resources.getString(R.string.subscribe)) { dialog, _ ->
                                            (activity as MainActivity).binding.bottomFloatingButton.isEnabled =
                                                true
                                            (activity as MainActivity).binding.bottomAppBar.isEnabled =
                                                true
                                            findNavController().navigate(R.id.subscribeFragment)
                                            dialog.dismiss()
                                        }
                                        alertDialog.setNeutralButton(resources.getString(R.string.later)) { dialog, _ ->
                                            (activity as MainActivity).binding.bottomFloatingButton.isEnabled =
                                                true
                                            (activity as MainActivity).binding.bottomAppBar.isEnabled =
                                                true
                                            dialog.dismiss()
                                        }
                                        alertDialog.setNegativeButton(resources.getString(R.string.dont_show)) { dialog, _ ->
                                            (activity as MainActivity).binding.bottomFloatingButton.isEnabled =
                                                true
                                            (activity as MainActivity).binding.bottomAppBar.isEnabled =
                                                true
                                            with(sharedPref.edit()) {
                                                putBoolean(IMPORT_CONFIRM, true)
                                                apply()
                                            }
                                            dialog.dismiss()
                                        }
                                    }
                                    alertDialog.show()
                                } else {
                                    (activity as MainActivity).binding.bottomFloatingButton.isEnabled =
                                        true
                                    (activity as MainActivity).binding.bottomAppBar.isEnabled = true
                                    with(sharedPref.edit()) {
                                        putBoolean(IMPORT_CONFIRM, true)
                                        apply()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    taskCollection.exception?.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(resources.getString(R.string.error_checking_tasks))
                                    .setMessage(
                                        resources.getString(R.string.check_your_internet)
                                    )
                                    .setPositiveButton(resources.getString(R.string.try_word)) { dialog, _ ->
                                        importFirebaseTasks(sharedPref)
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                                        dialog.cancel()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }
                        }
                }else{
                    (activity as MainActivity).binding.bottomFloatingButton.isEnabled = true
                    (activity as MainActivity).binding.bottomAppBar.isEnabled = true
                }
            }
        }
    }

    private fun downloadFirebaseList(sharedPref: SharedPreferences) {
        runBlocking {
            val loadingDialog = LoadingAlertFragment()
            loadingDialog.show(
                childFragmentManager, LoadingAlertFragment.TAG
            )

            //Download tasks
            userBackupTaskListReference
                ?.get()
                ?.addOnCompleteListener { taskCollection ->
                    if (taskCollection.isSuccessful) {
                        val currentTasksList: MutableList<String> = ArrayList()
                        val taskDocument = taskCollection.result
                        val mapFromTasks: MutableMap<String, Any>? = taskCollection.result.data
                        val downloadGotTasks: MutableList<TaskType> = ArrayList()

                        if (mapFromTasks != null) {
                            //Get map value from key task
                            for ((key) in mapFromTasks) {
                                currentTasksList.add(key)
                            }
                            for (task in currentTasksList) {
                                val taskType = TaskType(
                                    idTask = (taskDocument.getLong(
                                        "$task.${TaskType.COLUMN_ID}"
                                    )?.toInt()
                                        ?: mAdapter.currentList.last().idTask) + 100 + currentTasksList.indexOf(
                                        task
                                    ),
                                    title = taskDocument.getString("$task.${TaskType.COLUMN_TITLE}")
                                        ?: "Error getting title",
                                    description = taskDocument.getString("$task.${TaskType.COLUMN_DESCRIPTION}")
                                        ?: "Error getting description",
                                    priority = taskDocument.getLong("$task.${TaskType.COLUMN_PRIORITY}")
                                        ?.toInt() ?: 1,

                                    time = taskDocument.getLong("$task.${TaskType.COLUMN_TIME}")
                                        ?: GLOBAL_START_DATE,
                                    deadline = taskDocument.getLong("$task.${TaskType.COLUMN_DEADLINE}")
                                        ?: GLOBAL_START_DATE,
                                    lastOvercheck = taskDocument.getLong("$task.${TaskType.COLUMN_LAST_OVERCHECK}")
                                        ?: GLOBAL_START_DATE,
                                    created = (taskDocument.getLong("$task.${TaskType.COLUMN_CREATED}")
                                        ?: System.currentTimeMillis()) + currentTasksList.indexOf(
                                        task
                                    ),
                                    completed = taskDocument.getLong("$task.${TaskType.COLUMN_COMPLETED}")
                                        ?: GLOBAL_START_DATE,

                                    repeatMode = taskDocument.getLong("$task.${TaskType.COLUMN_REPEAT_MODE}")
                                        ?.toInt() ?: 0,
                                    missed = taskDocument.getBoolean("$task.${TaskType.COLUMN_MISSED}")
                                        ?: false,
                                    checked = taskDocument.getBoolean("$task.${TaskType.COLUMN_CHECKED}")
                                        ?: false,
                                    totalChecked = taskDocument.getLong("$task.${TaskType.COLUMN_TOTAL_CHECKED}")
                                        ?.toInt() ?: 0
                                )
                                if (taskType.time != GLOBAL_START_DATE) {
                                    if (taskType.time <= System.currentTimeMillis() && taskType.repeatMode == 0) {
                                        taskType.time = GLOBAL_START_DATE
                                    } else {
                                        alarmMethods.setReminder(
                                            requireContext(),
                                            taskType.idTask,
                                            taskType.repeatMode,
                                            taskType.time
                                        )
                                    }
                                }
                                if (taskType.deadline != GLOBAL_START_DATE) {
                                    if (taskType.deadline <= System.currentTimeMillis()) {
                                        taskType.missed = true
                                    } else {
                                        alarmMethods.setDeadline(
                                            requireContext(),
                                            taskType.idTask,
                                            taskType.deadline
                                        )
                                    }

                                }
                                downloadGotTasks.add(taskType)
                            }
                        }
                        val appList = mAdapter.currentList
                        downloadGotTasks.addAll(if (appList.isNotEmpty()) appList else arrayListOf())
                        downloadGotTasks.sortBy { it.created }

                        sharedViewModel.isMiddleList(
                            if (downloadGotTasks.size >= 50) downloadGotTasks[49].created else GLOBAL_START_DATE,
                            true
                        )
                        sharedViewModel.insertAllTasks(downloadGotTasks)
                        sharedViewModel.updateSyncState(SynchronizationState.COMPLETE_SYNC)

                        (activity as MainActivity).binding.bottomFloatingButton.isEnabled = true
                        (activity as MainActivity).binding.bottomAppBar.isEnabled = true
                        with(sharedPref.edit()) {
                            putBoolean(IMPORT_CONFIRM, true)
                            putBoolean(IMPORT_DOWNLOADED, true)
                            apply()
                        }
                        cancel(null)
                        loadingDialog.dismiss()
                    } else {
                        cancel(null)
                        loadingDialog.dismiss()
                        Toast.makeText(
                            context,
                            taskCollection.exception?.message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(resources.getString(R.string.error_importing_tasks))
                            .setMessage(
                                resources.getString(R.string.check_your_internet)
                            )
                            .setPositiveButton(resources.getString(R.string.try_word)) { dialog, _ ->
                                downloadFirebaseList(sharedPref)
                                dialog.dismiss()
                            }
                            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                                dialog.cancel()
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
        }
    }


    fun updateFirebaseCloudBackup(currentList: List<TaskType>) {
        if (Firebase.auth.currentUser?.isEmailVerified == true) {
            sharedViewModel.updateSyncState(
                SynchronizationState.PENDING_SYNC
            )
            userTaskListReference?.set(
                currentList.associateBy(
                    { (it.idTask + it.created).toString() },
                    { it })
            )?.addOnCompleteListener {
                if (it.isSuccessful) {
                    sharedViewModel.updateSyncState(
                        SynchronizationState.COMPLETE_SYNC
                    )
                } else {
                    sharedViewModel.updateSyncState(
                        SynchronizationState.ERROR_SYNC
                    )
                }
            }
            userInfoReference?.get()?.addOnCompleteListener { userInfo ->
                if (userInfo.isSuccessful) {
                    val lastSync = userInfo.result.getLong(KEY_USER_LAST_SYNC) ?: GLOBAL_START_DATE

                    if (System.currentTimeMillis() - lastSync >= AlarmManager.INTERVAL_DAY * 3) {
                        userInfoReference?.update(KEY_USER_LAST_SYNC, System.currentTimeMillis())
                        userBackupTaskListReference?.set(
                            currentList.associateBy(
                                { (it.idTask + it.created).toString() },
                                { it })
                        )?.addOnCompleteListener {
                            if (it.isSuccessful) {
                                sharedViewModel.updateSyncState(
                                    SynchronizationState.COMPLETE_SYNC
                                )
                            } else {
                                sharedViewModel.updateSyncState(
                                    SynchronizationState.ERROR_SYNC
                                )
                            }
                        }
                    } else if (System.currentTimeMillis() - lastSync >= AlarmManager.INTERVAL_DAY) {
                        userInfoReference?.update(KEY_USER_LAST_SYNC, System.currentTimeMillis())
                        userBackupTaskListReference?.set(
                            currentList.associateBy(
                                { (it.idTask + it.created).toString() },
                                { it }), SetOptions.merge()
                        )?.addOnCompleteListener {
                            if (it.isSuccessful) {
                                sharedViewModel.updateSyncState(
                                    SynchronizationState.COMPLETE_SYNC
                                )
                            } else {
                                sharedViewModel.updateSyncState(
                                    SynchronizationState.ERROR_SYNC
                                )
                            }
                        }
                    }
                }
            }


        } else {
            sharedViewModel.updateSyncState(
                SynchronizationState.DISABLED_SYNC
            )
        }
    }

    private fun updateFirebaseTotalCompletedTasks() {
        if (mAdapter.currentList.isNotEmpty()) {
            val userInfo: MutableMap<String, Any> = HashMap()
            lifecycleScope.launch {
                val totalCompleted = sharedViewModel.getTotalCompleted()
                userInfo[KEY_USER_COUNT_COMPLETED_TASKS] =
                    if (totalCompleted != 0) totalCompleted else return@launch
                userInfoReference?.update(userInfo)
            }
        }
    }

    private fun updateFirebasePrivateMode(privateModeState: Boolean) {
        val userInfo: MutableMap<String, Any> = HashMap()
        lifecycleScope.launch {
            userInfo[KEY_USER_PRIVATE_MODE] = privateModeState
            userInfoReference?.update(userInfo)
        }
    }


    //Adapter listener
    override fun onCheckBoxEvent(
        task: TaskType,
        isChecked: Boolean,
        isHold: Boolean,
        lastOverchecked: Long
    ) {
        if (task.title.isNotBlank()) {
            if (!isHold) {
                lifecycleScope.launch {
                    sharedViewModel.onTaskCheckedChanged(
                        task,
                        isChecked,
                        increase = false
                    )
                }
            } else {
                if (System.currentTimeMillis() - lastOverchecked >= AlarmManager.INTERVAL_DAY) {
                    (activity as MainActivity).apply {
                        vb?.vibrate(
                            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                        vb?.vibrate(
                            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    }
                    lifecycleScope.launch {
                        sharedViewModel.onTaskCheckedChanged(
                            task,
                            isChecked,
                            increase = true
                        )
                    }
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage(resources.getString(R.string.cant_overcheck_task))
                        .setNeutralButton(resources.getString(R.string.ok)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(resources.getString(R.string.cant_check_task))
                .setNeutralButton(resources.getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onTitleChanged(task: TaskType, title: String) {
        lifecycleScope.launch {
            sharedViewModel.onTitleChanged(task, title)
        }
    }

    override fun onEditTaskCLick(task: TaskType, taskView: View) {
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        val transitionName = getString(R.string.task_detail_transition_name)
        val extras = FragmentNavigatorExtras(taskView to transitionName)
        val directions = MainFragmentDirections.actionMainFragmentToTaskDetailsFragment(
            idTask = task.idTask
        )
        findNavController().navigate(directions, extras)
    }

    override fun promptPremium() {
        Toast.makeText(
            requireContext(),
            resources.getString(R.string.have_no_premium),
            Toast.LENGTH_SHORT
        ).show()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.subscribe))
            .setMessage(resources.getString(R.string.you_subscribe_tasks))
            .setPositiveButton(resources.getString(R.string.subscribe)) { dialog, _ ->
                findNavController().navigate(R.id.subscribeFragment)
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.not_now)) { dialog, _ ->

                dialog.cancel()
            }
            .show()

    }


    //WaterFragment
    private fun addingTask() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 300) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
        if (sharedViewModel.isListNotFull()) {
            lifecycleScope.launch {
                sharedViewModel.insertTask(TaskType())
            }
        } else {
            showingDialogMaximumTasks()
        }

    }

    private fun addingMultipleTasks() {
        val viewInflated: View = LayoutInflater.from(context)
            .inflate(
                R.layout.alertdialog_enter_amount_of_tasks,
                view as ViewGroup?,
                false
            )
        val enterAmount = viewInflated.findViewById<View>(R.id.input_amount) as TextInputEditText
        (viewInflated.findViewById<View>(R.id.input_amount_layout) as TextInputLayout).hint =
            resources.getString(R.string.enter_amount_hint, sharedViewModel.maxAddTask)
        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setView(viewInflated)
            .setTitle(resources.getString(R.string.enter_amount_title))
            .setPositiveButton(R.string.add_task) { _, _ ->
                val maxTasks = sharedViewModel.maxTasksCount +
                        if (sharedViewModel.premiumState.value == true) 50 else 0
                if (enterAmount.text?.isNotEmpty() == true) {
                    var enteredCount = enterAmount.text.toString().toInt()
                    if (sharedViewModel.isListNotFull()) {
                        if (enteredCount in 1..sharedViewModel.maxAddTask) {
                            if (enteredCount.plus(countOfTasks) > maxTasks
                            ) {
                                enteredCount = maxTasks - countOfTasks
                            }
                            sharedViewModel.insertAllTasks(
                                TaskType(),
                                enteredCount
                            )
                            Toast.makeText(
                                context,
                                resources.getQuantityString(
                                    R.plurals.tasks_been_added,
                                    enteredCount,
                                    enteredCount
                                ),
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
                            Toast.makeText(
                                context,
                                resources.getString(
                                    R.string.incorrect_add_tasks,
                                    sharedViewModel.maxAddTask
                                ),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    } else {
                        showingDialogMaximumTasks()
                    }
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun showingDialogMaximumTasks() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.reaching_max_tasks))
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }


    //System functions
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bottom_app_bar_main_menu, menu)
        sharedViewModel.allOnlyCompletedTasksSize.observe(viewLifecycleOwner) {
            menu.findItem(R.id.action_hide_completed_tasks).isEnabled = it > 0
        }
        sharedViewModel.allOnlyOverCompletedTasksSize.observe(viewLifecycleOwner) {
            menu.findItem(R.id.action_hide_overcompleted_tasks).isEnabled = it > 0

        }
        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                sharedViewModel.preferencesFlow.first().hideCompleted
            menu.findItem(R.id.action_hide_overcompleted_tasks).isChecked =
                sharedViewModel.preferencesFlow.first().hideOvercompleted
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                findNavController().currentDestination?.apply {
                    exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
                        duration =
                            resources.getInteger(R.integer.reply_motion_duration_large).toLong()
                    }
                    reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
                        duration =
                            resources.getInteger(R.integer.reply_motion_duration_large).toLong()
                    }
                }
                findNavController().navigate(MainFragmentDirections.actionMainFragmentToSearchFragment())
                true
            }
            R.id.action_hide_completed_tasks -> {
                item.isChecked = !item.isChecked
                sharedViewModel.onHideCompletedClick(item.isChecked)
                true
            }
            R.id.action_hide_overcompleted_tasks -> {
                item.isChecked = !item.isChecked
                sharedViewModel.onHideOvercompletedClick(item.isChecked)
                true
            }
            R.id.action_sort -> {
                val bottomActionsFragment = BottomActionsFragment()
                val bundle = Bundle()
                bundle.putInt("action", 1)
                bottomActionsFragment.arguments = bundle
                bottomActionsFragment.show(
                    requireActivity().supportFragmentManager,
                    bottomActionsFragment.tag
                )
                true
            }
            R.id.action_delete_tasks -> {
                val bottomActionsFragment = BottomActionsFragment()
                val bundle = Bundle()
                bundle.putInt("action", 0)
                bottomActionsFragment.arguments = bundle
                bottomActionsFragment.show(
                    requireActivity().supportFragmentManager,
                    bottomActionsFragment.tag
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onPause() {
        adView?.pause()
        super.onPause()

    }

    override fun onResume() {
        super.onResume()
        if (adView != null) {
            adView?.resume()
        }
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        binding.rvTasks.adapter = null
        adView = null
        _binding = null
    }
}