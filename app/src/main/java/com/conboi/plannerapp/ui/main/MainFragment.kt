package com.conboi.plannerapp.ui.main

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.view.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    }

    private var _binding: FragmentMainBinding? = null
    val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val mAdapter: TaskAdapter = TaskAdapter(this)
    private lateinit var adView: AdView

    //Firebase
    private var userInfoReference: DocumentReference? = null
    private var userTaskListReference: DocumentReference? = null

    var connection: Boolean? = null

    private var mLastClickTime: Long = 0
    private var countOfTasks: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        initUser()
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
        adView = binding.adView
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
                        if (dX > 50) {
                            val iconMarginLeft = (dX * 0.1F).coerceAtMost(70f).coerceAtLeast(0f)
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
        }
        setHasOptionsMenu(true)
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
            val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
            if (mainActivity.auth.currentUser != null) {
                mainActivity.auth.currentUser!!.reload()
                adView = binding.adView
                adView.loadAd(AdRequest.Builder().build())
                if (!sharedPref.getBoolean(
                        EMAIL_CONFIRM,
                        false
                    )
                ) {
                    if (!mainActivity.auth.currentUser!!.isEmailVerified) {
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
                    mainActivity.db.document("Users/${mainActivity.auth.currentUser!!.uid}")
                userTaskListReference =
                    mainActivity.db.document("Users/${mainActivity.auth.currentUser!!.uid}/TaskList/Tasks")
                if (!(sharedPref.getBoolean(IMPORT_CONFIRM, false))) {
                    //Get total completed
                    userInfoReference?.get()?.addOnCompleteListener { snapshot ->
                        if (snapshot.isSuccessful) {
                            if (snapshot.result.exists()) {
                                sharedViewModel.updateTotalCompleted(
                                    snapshot.result.getLong(KEY_USER_COUNT_COMPLETED_TASKS)
                                        ?.toInt() ?: 0
                                )
                            }
                        }
                    }
                    (activity as MainActivity).binding.bottomFloatingButton.isEnabled = false
                    (activity as MainActivity).binding.bottomAppBar.isEnabled = false
                    importFirebaseTasks(sharedPref)
                }
            }
        }
    }

    private fun importFirebaseTasks(sharedPref: SharedPreferences) {
        //Get tasks
        userTaskListReference?.get()
            ?.addOnCompleteListener { taskCollection ->
                if (taskCollection.isSuccessful) {
                    if (taskCollection.result.exists()) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(resources.getString(R.string.warning))
                            .setMessage(
                                resources.getString(R.string.backup_tasks_message)
                            )
                            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                                downloadFirebaseTaskList(sharedPref)
                                dialog.dismiss()
                            }
                            .setNegativeButton(resources.getString(R.string.deny)) { dialog, _ ->
                                (activity as MainActivity).binding.bottomFloatingButton.isEnabled =
                                    true
                                (activity as MainActivity).binding.bottomAppBar.isEnabled = true
                                with(sharedPref.edit()) {
                                    putBoolean(IMPORT_CONFIRM, true)
                                    apply()
                                }
                                dialog.dismiss()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        (activity as MainActivity).binding.bottomFloatingButton.isEnabled = true
                        (activity as MainActivity).binding.bottomAppBar.isEnabled = true
                        with(sharedPref.edit()) {
                            putBoolean(IMPORT_CONFIRM, true)
                            apply()
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        taskCollection.exception!!.message.toString(),
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
    }

    private fun downloadFirebaseTaskList(sharedPref: SharedPreferences) {
        runBlocking {
            //Set loading AlertDialog
            val loadingView: View = LayoutInflater.from(context)
                .inflate(
                    R.layout.alertdialog_loading_tasks,
                    view as ViewGroup?,
                    false
                )
            val loadingTasksAlertDialog = MaterialAlertDialogBuilder(requireContext())
                .setCancelable(false)
                .setTitle(resources.getString(R.string.loading))
                .setView(loadingView)

            val loadingDialog = loadingTasksAlertDialog.create()
            loadingDialog.show()
            //Download tasks
            userTaskListReference
                ?.get()
                ?.addOnCompleteListener { taskCollection ->
                    if (taskCollection.isSuccessful) {
                        val currentTasksList: MutableList<String> = ArrayList()
                        val taskDocument = taskCollection.result
                        val mapFromTasks: MutableMap<String, Any>? = taskCollection.result.data
                        val downloadGotTasks: MutableList<TaskType> = ArrayList()

                        //Get map value from key task
                        for ((key) in mapFromTasks!!) {
                            currentTasksList.add(key)
                        }
                        for (task in currentTasksList) {
                            val idTask =
                                if (mAdapter.currentList.isNotEmpty()) {
                                    mAdapter.currentList[mAdapter.currentList.lastIndex].idTask + currentTasksList.indexOf(
                                        task
                                    )
                                } else {
                                    currentTasksList.indexOf(task) + 1
                                }
                            val taskType = TaskType(
                                idTask = idTask,
                                title = taskDocument.getString("$task.${TaskType.COLUMN_TITLE}")
                                    ?: "Error getting title",
                                description = taskDocument.getString("$task.${TaskType.COLUMN_DESCRIPTION}")
                                    ?: "Error getting description",
                                time = taskDocument.getLong("$task.${TaskType.COLUMN_TIME}")
                                    ?: GLOBAL_START_DATE,
                                deadline = taskDocument.getLong("$task.${TaskType.COLUMN_DEADLINE}")
                                    ?: GLOBAL_START_DATE,
                                repeatMode = taskDocument.getLong("$task.${TaskType.COLUMN_REPEAT_MODE}")
                                    ?.toInt() ?: 0,
                                priority = taskDocument.getLong("$task.${TaskType.COLUMN_PRIORITY}")
                                    ?.toInt() ?: 1,
                                checked = taskDocument.getBoolean("$task.${TaskType.COLUMN_CHECKED}")
                                    ?: false,
                                totalChecked = taskDocument.getLong("$task.${TaskType.COLUMN_TOTAL_CHECKED}")
                                    ?.toInt() ?: 0,
                                created = taskDocument.getLong("$task.${TaskType.COLUMN_CREATED}")
                                    ?: System.currentTimeMillis(),
                                completed = taskDocument.getLong("$task.${TaskType.COLUMN_COMPLETED}")
                                    ?: GLOBAL_START_DATE,
                                missed = taskDocument.getBoolean("$task.${TaskType.COLUMN_MISSED}")
                                    ?: false
                            )
                            if (taskType.time != GLOBAL_START_DATE) {
                                if (taskType.time - System.currentTimeMillis() <= System.currentTimeMillis() && taskType.repeatMode == 0) {
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
                        sharedViewModel.insertAllTasks(downloadGotTasks)
                        (activity as MainActivity).binding.bottomFloatingButton.isEnabled = true
                        (activity as MainActivity).binding.bottomAppBar.isEnabled = true
                        with(sharedPref.edit()) {
                            putBoolean(IMPORT_CONFIRM, true)
                            apply()
                        }
                        loadingDialog.cancel()
                    } else {
                        loadingDialog.cancel()
                        Toast.makeText(
                            context,
                            taskCollection.exception!!.message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(resources.getString(R.string.error_importing_tasks))
                            .setMessage(
                                resources.getString(R.string.check_your_internet)
                            )
                            .setPositiveButton(resources.getString(R.string.try_word)) { dialog, _ ->
                                downloadFirebaseTaskList(sharedPref)
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
        if (Firebase.auth.currentUser!!.isEmailVerified) {
            userTaskListReference?.get()
                ?.addOnCompleteListener { userTaskList ->
                    sharedViewModel.updateSyncState(
                        SynchronizationState.PENDING_SYNC
                    )
                    if (userTaskList.isSuccessful) {
                        userTaskListReference!!.set(
                            currentList.associateBy({ it.idTask.toString() }, { it })
                        )
                            .addOnSuccessListener {
                                sharedViewModel.updateSyncState(
                                    SynchronizationState.COMPLETE_SYNC
                                )
                            }
                            .addOnFailureListener {
                                sharedViewModel.updateSyncState(
                                    SynchronizationState.ERROR_SYNC
                                )
                            }
                    } else {
                        sharedViewModel.updateSyncState(
                            SynchronizationState.ERROR_SYNC
                        )
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
                userInfo[KEY_USER_COUNT_COMPLETED_TASKS] = sharedViewModel.getTotalCompleted()
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
    override fun onCheckBoxEvent(task: TaskType, isChecked: Boolean, isHold: Boolean) {
        if (task.title.isNotBlank()) {
            if (!isHold) {
                lifecycleScope.launch {
                    sharedViewModel.onTaskCheckedChanged(
                        task,
                        isChecked,
                        increase = false,
                        requireContext()
                    )
                }
            } else {
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
                        increase = true,
                        requireContext()
                    )
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


    //WaterFragment
    private fun addingTask() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 250) {
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
                if (enterAmount.text?.isNotEmpty() == true) {
                    var enteredCount = enterAmount.text.toString().toInt()
                    if (sharedViewModel.isListNotFull()) {
                        if (enteredCount in 1..sharedViewModel.maxAddTask) {
                            if (enteredCount.plus(countOfTasks) > sharedViewModel.maxTasksCount) {
                                enteredCount = sharedViewModel.maxTasksCount - countOfTasks
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
        adView.pause()
        super.onPause()

    }

    override fun onResume() {
        super.onResume()
        if (adView != null) {
            adView.resume()
        }
    }

    override fun onDestroy() {
        try {
            adView.destroy()
        } catch (e: Exception) {

        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        binding.rvTasks.adapter = null
        _binding = null
    }
}