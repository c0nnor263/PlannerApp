package com.conboi.plannerapp.ui.main

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.*
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
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.SynchronizationState
import com.conboi.plannerapp.data.dataStore
import com.conboi.plannerapp.databinding.FragmentMainBinding
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.model.TaskType.TaskEntry
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.bottomsheet.BottomActionsFragment
import com.conboi.plannerapp.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.recyclerview.animators.FadeInAnimator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
const val IMPORT_CONFIRM = "IMPORT_CONFIRM"

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class MainFragment : BaseTabFragment(), TaskAdapter.OnTaskClickListener {
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

    //Firebase
    private var userInfoReference: DocumentReference? = null
    private var userTaskListReference: DocumentReference? = null

    var connection: Boolean? = null

    //FABs
    private var mLastClickTime: Long = 0

    //WaterFragment variables
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
        //Animation
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }

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
                    true
                }
            }
        }


        view.background.alpha = 35
        binding.lifecycleOwner = this
        binding.rvTasks.apply {
            mAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    binding.rvTasks.smoothScrollToPosition(positionStart)
                }
            })
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = FadeInAnimator().apply {
                changeDuration = 300
                addDuration = 100
                removeDuration = 100
            }
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
                    val snackBar =
                        Snackbar.make(
                            binding.clSnack,
                            resources.getString(R.string.task_deleted),
                            Snackbar.LENGTH_LONG
                        )
                    snackBar.setAction(resources.getString(R.string.undo)) {
                        sharedViewModel.onUndoDeleteClick(task)
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
                    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        val itemView: View = viewHolder.itemView

                        val p = Paint().also {
                            it.color = Color.RED
                        }
                        val d: Drawable =
                            ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24)!!
                        val icon = drawableToBitmap(d)

                        // Draw background
                        c.drawRect(
                            itemView.left.toFloat() + dX,
                            itemView.top.toFloat(),
                            itemView.left.toFloat(),
                            itemView.bottom.toFloat(),
                            p
                        )

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
            sortedTasks.observe(this@MainFragment.viewLifecycleOwner) { items ->
                items.let {
                    if (mAdapter.currentList.isNotEmpty()) {
                        updateFirebaseTotalCompletedTasks()
                    }
                    mAdapter.submitList(it)
                }
            }

            //LiveData list of tasks
            allTasks.observe(this@MainFragment.viewLifecycleOwner) {
                if (mAdapter.currentList.isNotEmpty()) {
                    updateFirebaseServerTaskList(it)
                }
            }
        }
        setHasOptionsMenu(true)
    }

    //Firebase
    private fun initUser() {
        val connectedRef = Firebase.database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                connection = connected
                if (mAdapter.currentList.isNotEmpty()) {
                    updateFirebaseServerTaskList(mAdapter.currentList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        (activity as MainActivity).apply {
            if (auth.currentUser != null) {
                userInfoReference = db.document("Users/${auth.currentUser!!.uid}")
                userTaskListReference =
                    db.document("Users/${auth.currentUser!!.uid}/TaskList/Tasks")

                lifecycleScope.launch {
                    Firebase.auth.currentUser?.apply {
                        val userInfo: MutableMap<String, Any> = HashMap()
                        userInfo[KEY_USER_ID] = uid
                        userInfo[KEY_USER_EMAIL] = email.toString()
                        userInfo[KEY_USER_NAME] = displayName ?: email.toString()
                        userInfo[KEY_USER_PHOTO_URL] = photoUrl.toString()
                        userInfo[KEY_USER_EMAIL_CONFIRM] = isEmailVerified
                        userInfo[KEY_USER_PRIVATE_MODE] =
                            requireContext().dataStore.data.first()[PreferencesManager.PreferencesKeys.PRIVATE_MODE]
                                ?: false
                        userInfoReference!!.set(userInfo, SetOptions.merge())
                    }
                }
                val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
                if (!(sharedPref.getBoolean(IMPORT_CONFIRM, false))) {
                    importFirebaseTasks(sharedPref)
                }
            }
        }
    }

    private fun importFirebaseTasks(sharedPref: SharedPreferences) {
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
                                with(sharedPref.edit()) {
                                    putBoolean(IMPORT_CONFIRM, true)
                                    apply()
                                }
                                dialog.dismiss()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
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
                        val alarmManager = ContextCompat.getSystemService(
                            requireContext(),
                            AlarmManager::class.java
                        )
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
                                title = taskDocument.getString("$task.${TaskEntry.COLUMN_TITLE}")
                                    ?: "Error getting title",
                                description = taskDocument.getString("$task.${TaskEntry.COLUMN_DESCRIPTION}")
                                    ?: "Error getting description",
                                time = taskDocument.getLong("$task.${TaskEntry.COLUMN_TIME}")
                                    ?: GLOBAL_START_DATE,
                                deadline = taskDocument.getLong("$task.${TaskEntry.COLUMN_DEADLINE}")
                                    ?: GLOBAL_START_DATE,
                                repeatMode = taskDocument.getLong("$task.${TaskEntry.COLUMN_REPEAT_MODE}")
                                    ?.toInt() ?: 0,
                                priority = taskDocument.getLong("$task.${TaskEntry.COLUMN_PRIORITY}")
                                    ?.toInt() ?: 1,
                                checked = taskDocument.getBoolean("$task.${TaskEntry.COLUMN_CHECKED}")
                                    ?: false,
                                totalChecked = taskDocument.getLong("$task.${TaskEntry.COLUMN_TOTAL_CHECKED}")
                                    ?.toInt() ?: 0,
                                created = taskDocument.getLong("$task.${TaskEntry.COLUMN_CREATED}")
                                    ?: System.currentTimeMillis(),
                                completed = taskDocument.getLong("$task.${TaskEntry.COLUMN_COMPLETED}")
                                    ?: GLOBAL_START_DATE
                            )
                            if (taskType.time != GLOBAL_START_DATE) {
                                alarmManager!!.setOrUpdateReminder(
                                    requireContext(),
                                    taskType.idTask,
                                    taskType.title,
                                    taskType.time,
                                    taskType.repeatMode
                                )
                            }
                            downloadGotTasks.add(taskType)
                        }
                        sharedViewModel.insertAllTasks(downloadGotTasks)
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

    private fun updateFirebaseServerTaskList(currentList: List<TaskType>) {
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
                    sharedViewModel.onTaskCheckedChanged(task, isChecked, increase = false)
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
                    sharedViewModel.onTaskCheckedChanged(task, isChecked, increase = true)
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
        val directions = SearchFragmentDirections.actionGlobalTaskDetailsFragment(
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
                findNavController().navigate(R.id.searchFragment)
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

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        binding.rvTasks.adapter = null
        _binding = null
    }
}