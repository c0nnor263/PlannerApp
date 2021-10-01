package com.conboi.plannerapp.ui.main

import android.content.Context
import android.content.SharedPreferences
import android.os.*
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.TaskAdapter
import com.conboi.plannerapp.data.SortOrder
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.data.model.TaskType.TaskEntry
import com.conboi.plannerapp.databinding.FragmentMainBinding
import com.conboi.plannerapp.ui.FirebaseUserLiveData
import com.conboi.plannerapp.utils.BaseTabFragment
import com.conboi.plannerapp.utils.exhaustive
import com.conboi.plannerapp.utils.hideKeyboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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
        const val KEY_USER_COUNT_COMPLETED_TASKS = "user_count_completed"
    }

    private var _binding: FragmentMainBinding? = null
    val binding get() = _binding!!
    private val viewModel: SharedViewModel by viewModels()

    private var vb: Vibrator? = null


    private val mAdapter = TaskAdapter(this)

    //Firebase
    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var userInfoReference: DocumentReference? = null
    private var userTaskListReference: DocumentReference? = null

    //FABs
    private var mLastClickTime: Long = 0

    //WaterFragment variables
    private var countOfTasks: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requireActivity().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as Vibrator?
            } else {
                requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        observeAuthenticationState()
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Are you sure you want to quit?")
                .setPositiveButton("Exit") { _, _ ->
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
        //Material animation
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }



        requireActivity().bottom_floating_button.apply {
            setOnClickListener {
                addingTask()
            }
            setOnLongClickListener {
                vb!!.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                addingMultipleTasks()
                vb!!.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                true
            }
        }


        view.background.alpha = 35
        binding.lifecycleOwner = this
        binding.recyclerViewTasks.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            setItemViewCacheSize(15)
            //Swipe delete task
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.RIGHT
            ) {
                override fun onSelectedChanged(
                    viewHolder: RecyclerView.ViewHolder?,
                    actionState: Int
                ) {
                    super.onSelectedChanged(viewHolder, actionState)

                    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        vb!!.vibrate(
                            VibrationEffect.createOneShot(
                                50,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    }
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    vb!!.vibrate(
                        VibrationEffect.createOneShot(
                            25,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                    val task = mAdapter.currentList[viewHolder.bindingAdapterPosition]
                    viewModel.swipeDeleteTask(task)
                    requireActivity().bottom_app_bar.performShow()
                }

            }).attachToRecyclerView(this)

        }

        viewModel.apply {
            //LiveData size of completed tasks
            allCompletedTasksSize.observe(viewLifecycleOwner) {
                if (it > 0) {
                    requireActivity().bottom_app_bar_count_of_completed.visibility = View.VISIBLE
                    requireActivity().bottom_app_bar_count_of_completed.text =
                        resources.getString(R.string.count_of_completed, it)
                } else {
                    requireActivity().bottom_app_bar_count_of_completed.visibility = View.GONE
                }
            }
            //LiveData size of tasks list
            allTasksSize.observe(this@MainFragment.viewLifecycleOwner) {
                requireActivity().bottom_app_bar_count_of_tasks.text =
                    resources.getString(
                        R.string.count_of_tasks,
                        it,
                        viewModel.maxTasksCount
                    )
                countOfTasks = it
            }
            //LiveData list of tasks
            allTasks.observe(this@MainFragment.viewLifecycleOwner) { items ->
                items.let {
                    if (mAdapter.currentList.isNotEmpty()) {
                        updateServerTaskList(it)
                        updateTotalCompletedTasks()
                    }
                    mAdapter.submitList(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.taskEvent.collect { event ->
                when (event) {
                    is SharedViewModel.TasksEvent.ShowUndoDeleteTaskMessage -> {
                        val snackBar =
                            Snackbar.make(
                                view.findViewById(R.id.myCoordinatorLayout),
                                "Task deleted",
                                Snackbar.LENGTH_LONG
                            )
                        snackBar.setAction("UNDO") {
                            viewModel.onUndoDeleteClick(event.task)
                        }
                        snackBar.show()
                    }
                }.exhaustive

            }
        }
        setHasOptionsMenu(true)
    }


    //Firebase
    private fun observeAuthenticationState() {
        viewModel.authenticationState.observe(viewLifecycleOwner) { authenticationState ->
            if (authenticationState == FirebaseUserLiveData.AuthenticationState.UNAUTHENTICATED) {
                findNavController().navigate(R.id.loginFragment)
            } else {
                initUser()
            }
        }
    }

    private fun initUser() {
        userInfoReference = db.document("Users/${auth.currentUser!!.uid}")
        userTaskListReference = db.document("Users/${auth.currentUser!!.uid}/TaskList/Tasks")
        Firebase.auth.currentUser?.apply {
            val userInfo: MutableMap<String, Any> = HashMap()
            userInfo[KEY_USER_ID] = uid
            userInfo[KEY_USER_EMAIL] = email.toString()
            userInfo[KEY_USER_NAME] = displayName ?: email.toString()
            userInfo[KEY_USER_PHOTO_URL] = photoUrl.toString()
            userInfo[KEY_USER_EMAIL_CONFIRM] = isEmailVerified
            userInfoReference!!.set(userInfo, SetOptions.merge())
        }
        importTasks()
    }

    private fun importTasks() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        if (!(sharedPref.getBoolean(IMPORT_CONFIRM, false))) {
            userInfoReference?.get()?.addOnCompleteListener { snapshot ->
                if (snapshot.isSuccessful) {
                    if (snapshot.result.exists()) {
                        viewModel.onDownloadTotalCompleted(
                            snapshot.result.getLong(KEY_USER_COUNT_COMPLETED_TASKS)
                                ?.toInt() ?: 0
                        )
                    }
                }
            }
            userTaskListReference?.get()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (task.result.exists()) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Warning!")
                                .setMessage(
                                    "It seems that you have backup of previous tasks, count of completed etc.\n" +
                                            "\nWould you like to restore that?" +
                                            "\n(\"No\" - means overriding backup)"
                                )
                                .setPositiveButton("Yes") { dialog, _ ->
                                    downloadTaskList(sharedPref)
                                    dialog.dismiss()
                                }
                                .setNegativeButton("No") { dialog, _ ->
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
                            task.exception!!.message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Error check your backup tasks!")
                            .setMessage(
                                "Check your internet connection and try again.\n\n"
                            )
                            .setPositiveButton("Try") { dialog, _ ->
                                importTasks()
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.cancel()
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
        }
    }

    private fun downloadTaskList(sharedPref: SharedPreferences) {
        runBlocking {
            val loadingView: View = LayoutInflater.from(context)
                .inflate(
                    R.layout.alertdialog_loading_tasks_fragment_main,
                    view as ViewGroup?,
                    false
                )
            val loadingTasksAlertDialog = MaterialAlertDialogBuilder(requireContext())
            loadingTasksAlertDialog.apply {
                setCancelable(false)
                setMessage("Loading...")
                setView(loadingView)
            }
            val loadingDialog = loadingTasksAlertDialog.show()
            loadingDialog.show()
            userTaskListReference
                ?.get()
                ?.addOnCompleteListener { taskCollection ->
                    if (taskCollection.isSuccessful) {
                        val tasks: MutableList<String> = ArrayList()
                        val map: MutableMap<String, Any>? = taskCollection.result.data
                        val document = taskCollection.result
                        for ((key) in map!!) {
                            tasks.add(key)
                        }
                        for (i in 0 until tasks.size) {
                            val idTask =
                                if (mAdapter.currentList.isNotEmpty()) {
                                    mAdapter.currentList[mAdapter.currentList.lastIndex].idTask + i
                                } else {
                                    i + 1
                                }
                            val taskType = TaskType(
                                idTask = idTask,
                                title = document.getString("${tasks[i]}.${TaskEntry.COLUMN_TITLE}")!!,
                                description = document.getString("${tasks[i]}.${TaskEntry.COLUMN_DESCRIPTION}")!!,
                                time = document.getLong("${tasks[i]}.${TaskEntry.COLUMN_TIME}")!!
                                    .toInt(),
                                priority = document.getLong("${tasks[i]}.${TaskEntry.COLUMN_PRIORITY}")!!
                                    .toInt(),
                                checked = document.getBoolean("${tasks[i]}.${TaskEntry.COLUMN_CHECKED}")!!,
                                created = document.getLong("${tasks[i]}.${TaskEntry.COLUMN_CREATED}")!!,
                                completed = document.getLong("${tasks[i]}.${TaskEntry.COLUMN_COMPLETED}")!!
                            )
                            viewModel.downloadTask(taskType)
                        }

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
                            .setTitle("Error importing your tasks!")
                            .setMessage(
                                "Check your internet connection and try again.\nYou can cancel import"
                            )
                            .setPositiveButton("Try") { dialog, _ ->
                                downloadTaskList(sharedPref)
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->

                                dialog.cancel()
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
        }
    }

    private fun updateServerTaskList(taskList: List<TaskType>) {
        userTaskListReference?.get()
            ?.addOnCompleteListener {
                userTaskListReference!!.set(
                    taskList.associateBy({ it.idTask.toString() }, { it })
                )
            }
    }

    private fun updateTotalCompletedTasks() {
        if (mAdapter.currentList.isNotEmpty()) {
            val userInfo: MutableMap<String, Any> = HashMap()
            lifecycleScope.launch {
                userInfo[KEY_USER_COUNT_COMPLETED_TASKS] = viewModel.getTotalCompleted()
                userInfoReference?.update(userInfo)
            }
        }
    }

    //Adapter listener
    override fun onCheckBoxClick(taskType: TaskType, isChecked: Boolean) {
        lifecycleScope.launch {
            if (taskType.title!!.isNotBlank()) {
                viewModel.onTaskCheckedChanged(taskType, isChecked)
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("You can't check this task. Title is empty")
                    .setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    override fun onTitleChanged(taskType: TaskType, title: String) {
        lifecycleScope.launch {
            viewModel.onTitleChanged(taskType, title)
        }
    }

    override fun onEditTaskCLick(taskView: View, taskType: TaskType) {
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        val transitionName = getString(R.string.task_detail_transition_name)
        val extras = FragmentNavigatorExtras(taskView to transitionName)
        val directions = SearchFragmentDirections.actionGlobalTaskDetailsFragment(
            idTask = taskType.idTask
        )
        findNavController().navigate(directions, extras)
    }


    //Custom functions
    private fun preventOnClick() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 250) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
    }

    private fun addingTask() {
        if (viewModel.isListNotFull()) {
            preventOnClick()
            lifecycleScope.launch {
                viewModel.insertTask(TaskType(title = "", description = ""))
            }
        } else {
            showingDialogMaximumTasks()
        }
    }

    private fun addingMultipleTasks() {
        preventOnClick()
        val viewInflated: View = LayoutInflater.from(context)
            .inflate(
                R.layout.alertdialog_enter_amount_of_tasks_fragment_main,
                view as ViewGroup?,
                false
            )
        val enterAmount = viewInflated.findViewById<View>(R.id.input_amount) as EditText
        MaterialAlertDialogBuilder(
            requireContext(),
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
        )
            .setView(viewInflated)
            .setTitle("Enter an amount of tasks")
            .setPositiveButton(R.string.add_task) { _, _ ->
                if (enterAmount.text.isNotEmpty()) {
                    var enteredCount = enterAmount.text.toString().toInt()
                    if (viewModel.isListNotFull()) {
                        if (enteredCount in 1..viewModel.maxTasksCount) {

                            if (enteredCount.plus(countOfTasks) > viewModel.maxTasksCount) {
                                enteredCount = viewModel.maxTasksCount - countOfTasks
                            }
                            viewModel.insertTasks(
                                TaskType(title = "", description = ""),
                                enteredCount
                            )
                            Toast.makeText(
                                context,
                                "$enteredCount tasks has been added",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
                            Toast.makeText(
                                context,
                                "Incorrect, max amount - ${viewModel.maxTasksCount}",
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
            .setTitle("You have reached your maximum of tasks")
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }


    //System functions
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bottom_app_bar_main_menu, menu)
        viewModel.allCompletedTasksSize.observe(viewLifecycleOwner) {
            menu.findItem(R.id.action_hide_completed_tasks).isEnabled = it > 0
            menu.findItem(R.id.action_delete_completed_tasks).isEnabled = it > 0
            menu.findItem(R.id.action_sort_by_date_completed).isEnabled = it > 0
        }
        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                viewModel.preferencesFlow.first().hideCompleted
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
            R.id.action_sort_by_title -> {
                viewModel.onSortOrderSelected(SortOrder.BY_TITLE)
                true
            }
            R.id.action_sort_by_date_created -> {
                viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                true
            }
            R.id.action_sort_by_date_completed -> {
                viewModel.onSortOrderSelected(SortOrder.BY_COMPLETE)
                true
            }
            R.id.action_hide_completed_tasks -> {
                item.isChecked = !item.isChecked
                viewModel.onHideCompletedClick(item.isChecked)
                true
            }
            R.id.action_delete_completed_tasks -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirm deletion")
                    .setMessage("Do you want to delete completed tasks?")
                    .setPositiveButton("Confirm") { _, _ ->
                        viewModel.deleteCompletedTasks()
                        Toast.makeText(
                            requireContext(),
                            "Completed tasks has been successfully deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
                    .show()

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        _binding = null
    }
}