package com.conboi.plannerapp.ui.main

import android.app.AlarmManager
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.TaskAdapter
import com.conboi.plannerapp.adapter.TaskTypeDiffCallback
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.databinding.FragmentMainBinding
import com.conboi.plannerapp.interfaces.TaskListInterface
import com.conboi.plannerapp.interfaces.dialog.InputTaskAmountCallback
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.bottomsheet.BottomActionFragment
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.shared.LoadingDialogFragment
import com.conboi.plannerapp.utils.shared.firebase.FirebaseUserLiveData
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.android.play.core.review.ReviewManagerFactory
import com.qonversion.android.sdk.Qonversion
import com.qonversion.android.sdk.QonversionError
import com.qonversion.android.sdk.QonversionPermissionsCallback
import com.qonversion.android.sdk.dto.QPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.DateFormat
import javax.inject.Inject


@AndroidEntryPoint
class MainFragment @Inject constructor() : BaseTabFragment(), TaskListInterface,
    InputTaskAmountCallback, MenuProvider {
    @Inject
    lateinit var diffCallback: TaskTypeDiffCallback

    private var _binding: FragmentMainBinding? = null
    val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private lateinit var mAdapter: TaskAdapter
    private lateinit var mainActivity: MainActivity

    private var adView: AdView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        mAdapter = TaskAdapter(this, diffCallback)
        mainActivity = (activity as MainActivity)

        mainActivity.binding.fabMain.apply {
            setOnClickListener {
                addingTask()
                (drawable as AnimatedVectorDrawable).start()
            }
            setOnLongClickListener {
                mainActivity.vibrateDefaultAmplitude(2, HOLD_VIBRATION)
                addingMultipleTasks()
                (drawable as AnimatedVectorDrawable).start()
                true
            }
        }
        mainActivity.onBackPressedDispatcher.addCallback(this) {
            viewModel.sendExitEvent()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        // Animation
        postponeEnterTransition()

        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        view.background.alpha = 35

        initRv()

        viewModel.premiumState.observe(viewLifecycleOwner) {
            if (it) {
                hideAdView(
                    binding.adView,
                    viewToPadding = binding.rvTasks,
                    8
                )
            } else {
                adView =
                    showAdView(
                        requireContext(),
                        binding.adView,
                        viewToPadding = binding.rvTasks,
                        55
                    )
                binding.rvTasks.scrollToPosition(0)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Events observer
                viewModel.uiState.collect { currentState ->
                    when (currentState) {
                        MainViewModel.MainFragmentEvent.ShowExit -> {
                            showExitDialog()
                            viewModel.sendExitEvent(null)
                        }
                        MainViewModel.MainFragmentEvent.ShowRate -> {
                            showRateDialog()
                            viewModel.sendRateUsEvent(null)
                        }
                        MainViewModel.MainFragmentEvent.ShowErrorMaxTask -> {
                            showErrorMaxTaskDialog()
                            viewModel.sendErrorMaxTaskEvent(null)
                        }
                        MainViewModel.MainFragmentEvent.ShowErrorImportTask -> {
                            showErrorImportTaskDialog()
                            viewModel.sendErrorImportTaskEvent(null)
                        }
                        MainViewModel.MainFragmentEvent.ShowErrorCheckingTask -> {
                            showErrorCheckingTaskDialog()
                            viewModel.sendErrorCheckingTaskEvent(null)
                        }
                        MainViewModel.MainFragmentEvent.ShowEmailNotConfirmed -> {
                            showEmailNotConfirmedDialog()
                            viewModel.sendEmailNotConfirmedEvent(null)
                        }
                        MainViewModel.MainFragmentEvent.ShowImportServerTasks -> {
                            importServerTasks()
                            viewModel.sendImportServerTasksEvent(null)
                        }
                        MainViewModel.MainFragmentEvent.ShowCantCheck -> {
                            showCantCheckDialog(requireContext())
                            viewModel.sendCantCheckEvent(null)
                        }
                        MainViewModel.MainFragmentEvent.ShowCantOvercheck -> {
                            showCantOvercheckDialog(requireContext())
                            viewModel.sendCantOvercheckEvent(null)
                        }
                        is MainViewModel.MainFragmentEvent.ShowSync -> {
                            showSyncDialog(
                                currentState.premium,
                                currentState.lastSyncString
                            )
                            viewModel.sendSyncEvent(null, null, null)
                        }
                        is MainViewModel.MainFragmentEvent.SwipeDeleteTask -> {
                            Snackbar.make(
                                binding.clSnack,
                                resources.getString(R.string.task_deleted),
                                Snackbar.LENGTH_LONG
                            )
                                .setAction(resources.getString(R.string.undo)) {
                                    viewModel.onUndoDeleteClick(
                                        requireContext(),
                                        currentState.task
                                    )
                                }
                                .show()
                            mainActivity.showBottomAppBar()

                            viewModel.sendSwipeDeleteTaskEvent(null, null, null)
                        }
                        null -> {}
                    }
                }
            }
        }


        viewModel.authState.observe(viewLifecycleOwner) {
            if (it == FirebaseUserLiveData.AuthenticationState.AUTHENTICATED) viewModel.initUser()
        }


        // List of sorted tasks
        viewModel.sortedTasks.observe(viewLifecycleOwner) {
            if (mAdapter.currentList.isNotEmpty()) {
                viewModel.uploadTasksWithBackup(it)
            }
            mAdapter.submitList(it)
            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }

        // Size of tasks list
        viewModel.taskSize.observe(viewLifecycleOwner) {
            binding.tvNoTasks.visibility = if (it != 0) View.GONE else View.VISIBLE
        }

        // Tasks middle of list
        viewModel.middleListTime.observe(viewLifecycleOwner) {
            mAdapter.updateMiddleTime(it)
        }

        // Premium state
        viewModel.premiumState.observe(viewLifecycleOwner) {
            mAdapter.updatePremiumState(it)
        }
        setHasOptionsMenu(true)
        mainActivity.addMenuProvider(this)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.b_app_bar_main_menu, menu)
        viewModel.allCompletedTaskSize.observe(viewLifecycleOwner) {
            menu.findItem(R.id.action_hide_completed_tasks).isEnabled = it > 0
        }
        viewModel.allOvercompletedTaskSize.observe(viewLifecycleOwner) {
            menu.findItem(R.id.action_hide_overcompleted_tasks).isEnabled = it > 0
        }

        viewModel.hideCompleted.observe(viewLifecycleOwner) {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked = it
        }
        viewModel.hideOvercompleted.observe(viewLifecycleOwner) {
            menu.findItem(R.id.action_hide_overcompleted_tasks).isChecked = it
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_search -> {
                navigateToSearchFragment()
                true
            }
            R.id.action_hide_completed_tasks -> {
                menuItem.isChecked = !menuItem.isChecked
                viewModel.onHideCompletedClick(menuItem.isChecked)
                true
            }
            R.id.action_hide_overcompleted_tasks -> {
                menuItem.isChecked = !menuItem.isChecked
                viewModel.onHideOvercompletedClick(menuItem.isChecked)
                true
            }
            R.id.action_sort -> {
                showBottomActionFragment(BottomAction.SORT)
                true
            }
            R.id.action_delete_tasks -> {
                showBottomActionFragment(BottomAction.DELETE)
                true
            }
            else -> false
        }
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.windowInsetsController?.hide(WindowInsets.Type.ime())
        } else {
            hideKeyboard(requireActivity())
        }
        mainActivity.removeMenuProvider(this)
        _binding = null
        adView = null
    }


    // Adapter listener
    override fun onCheckBoxEvent(
        task: TaskType,
        isChecked: Boolean,
        isHold: Boolean,
    ) {
        lifecycleScope.launch {
            if (task.title.isNotBlank()) {
                if (isHold) {
                    if (System.currentTimeMillis() - task.lastOvercheck >= AlarmManager.INTERVAL_DAY) {
                        mainActivity.vibrateDefaultAmplitude(2, HOLD_VIBRATION)
                        viewModel.onTaskCheckedChanged(
                            task,
                            isChecked,
                            increase = true
                        )
                    } else {
                        viewModel.sendCantOvercheckEvent()
                    }
                } else {
                    viewModel.onTaskCheckedChanged(
                        task,
                        isChecked,
                        increase = false
                    )
                }
            } else {
                viewModel.sendCantCheckEvent()
            }
        }
    }

    override fun onTitleChanged(task: TaskType, title: String) {
        lifecycleScope.launch {
            viewModel.onTitleChanged(task, title)
        }
    }

    override fun onClick(view: View, id: Int) = navigateToTaskDetailFragment(id, view)

    override fun onHold() {}

    override fun showPremiumDealDialog() {
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
            .setNegativeButton(resources.getString(R.string.not_now)) { dialog, _ -> dialog.cancel() }
            .show()
    }

    override fun beginInsert(inputAmount: String) {
        val enteredCount = inputAmount.toInt()

        viewModel.increaseRateUs(enteredCount)
        viewModel.insertMultiTasks(
            inputAmount
        ) { result, error ->
            if (error == null) {
                result?.let {
                    Toast.makeText(
                        requireContext(),
                        resources.getQuantityString(
                            R.plurals.tasks_been_added,
                            result,
                            result
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                when (error.message) {
                    InsertMultipleTaskError.MAXIMUM.name -> {
                        viewModel.sendErrorMaxTaskEvent()
                    }
                    InsertMultipleTaskError.INCORRECT.name -> {
                        Toast.makeText(
                            context,
                            resources.getString(
                                R.string.incorrect_add_tasks,
                                MAX_ADD_TASK
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }


    private fun initRv() = with(binding.rvTasks) {
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
                    mainActivity.vibrateDefaultAmplitude(1)
                }
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.3f

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if ((viewHolder as TaskAdapter.ViewHolder).binding.tietTitle.isFocused) return ItemTouchHelper.ACTION_STATE_IDLE
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                mainActivity.vibrateDefaultAmplitude(2)

                val task = mAdapter.currentList[viewHolder.bindingAdapterPosition]
                viewModel.sendSwipeDeleteTaskEvent(requireContext(), task)
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
                    val paint = Paint().also {
                        it.color = Color.RED
                    }
                    val drawable: Drawable =
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24)!!
                    val icon = drawableToBitmap(drawable)

                    // Draw background
                    if (dX > 0.0) {
                        val rectF = RectF(
                            itemView.left.toFloat() - dX * 0.3F,
                            itemView.top.toFloat(),
                            itemView.right.toFloat() + dX * 0.3F,
                            itemView.bottom.toFloat()
                        )
                        c.drawRoundRect(rectF, 40F, 40F, paint)
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
                            paint
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

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)
            mAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    (binding.rvTasks.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        positionStart,
                        0
                    )
                }
            })
        }
    }

    // Firebase
    private fun importServerTasks() {
        Qonversion.checkPermissions(object : QonversionPermissionsCallback {
            override fun onSuccess(permissions: Map<String, QPermission>) {
                val premiumPermission = permissions[MainActivity.PREMIUM_PERMISSION]
                if (viewModel.getUser()?.isEmailVerified == true) {
                    showSyncAbility(
                        premiumPermission != null && premiumPermission.isActive()
                    )
                }
            }

            override fun onError(error: QonversionError) {
                showErrorToast(requireContext(), Exception(error.additionalMessage))
            }
        })
    }

    private fun downloadServerList() = runBlocking {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.isCancelable = false
        loadingDialog.show(
            childFragmentManager, LoadingDialogFragment.TAG
        )

        //Download tasks
        viewModel.downloadTasks(
            requireContext()
        ) { _, error ->
            if (error == null) {
                viewModel.successImport()
                cancel(null)
                loadingDialog.dismiss()
            } else {
                showErrorToast(requireContext(), error)
                cancel(null)
                loadingDialog.dismiss()
                viewModel.sendErrorImportTaskEvent()
            }
        }
    }


    //Task actions
    private var lastClickTime: Long = 0
    private fun addingTask() {
        if (SystemClock.elapsedRealtime() - lastClickTime < 300) return
        lastClickTime = SystemClock.elapsedRealtime()

        if (viewModel.isListNotFull()) {
            lifecycleScope.launch {
                viewModel.insertTask()
                viewModel.increaseRateUs()
            }
        } else {
            viewModel.sendErrorMaxTaskEvent()
        }
    }

    private fun addingMultipleTasks() {
        val inputTaskAmountDialogFragment = InputTaskAmountDialogFragment(this)
        inputTaskAmountDialogFragment.show(parentFragmentManager, InputTaskAmountDialogFragment.TAG)
    }

    private fun navigateToTaskDetailFragment(id: Int, view: View) {
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        val transitionName = getString(R.string.task_detail_transition_name)
        val extras = FragmentNavigatorExtras(view to transitionName)
        val directions = MainFragmentDirections.actionMainFragmentToTaskDetailsFragment(
            idTask = id
        )
        findNavController().navigate(directions, extras)
    }

    private fun navigateToSearchFragment() {
        findNavController().currentDestination?.apply {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
                duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            }
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
                duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            }
        }
        val direction = MainFragmentDirections.actionMainFragmentToSearchFragment()
        findNavController().navigate(direction)
    }

    private fun navigateToPlayStore(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
        val uri = Uri.parse("market://details?id=" + requireContext().packageName)
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }


    fun showSyncAbility(premium: Boolean) {
        viewModel.downloadLatestBackupInfo { result, error ->
            if (error == null) {
                if (result != null && result != 0L) {
                    val lastSyncString =
                        DateFormat.getDateTimeInstance(
                            DateFormat.DEFAULT,
                            DateFormat.DEFAULT,
                            resources.configuration.locales[0]
                        ).format(result)
                    viewModel.sendSyncEvent(premium, lastSyncString!!)
                } else {
                    viewModel.updateImportConfirm(true)
                }
            } else {
                showErrorToast(requireContext(), error)
                viewModel.sendErrorCheckingTaskEvent()
            }
        }
    }

    private fun showBottomActionFragment(action: BottomAction) {
        val bottomActionFragment = BottomActionFragment()
        val bundle = Bundle()
        bundle.putString("action", action.name)
        bottomActionFragment.arguments = bundle
        bottomActionFragment.show(
            parentFragmentManager,
            bottomActionFragment.tag
        )
    }

    private fun showSyncDialog(premium: Boolean, lastSyncString: String) {
        val alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.warning))
            .setCancelable(false)

        if (premium) {
            // Premium
            alertDialog.apply {
                setMessage(
                    resources.getString(
                        R.string.backup_tasks_message,
                        lastSyncString
                    )
                )
                setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                    downloadServerList()
                    dialog.dismiss()
                }
                setNegativeButton(resources.getString(R.string.deny)) { dialog, _ ->
                    viewModel.updateImportConfirm(true)
                    dialog.dismiss()
                }
            }
        } else {
            // Non Premium
            alertDialog.apply {
                setMessage(
                    resources.getString(
                        R.string.backup_tasks_message_no_prem,
                        lastSyncString
                    )
                )
                setPositiveButton(resources.getString(R.string.subscribe)) { dialog, _ ->
                    findNavController().navigate(R.id.subscribeFragment)
                    dialog.dismiss()
                }
                setNeutralButton(resources.getString(R.string.later)) { dialog, _ ->
                    dialog.dismiss()
                }
                setNegativeButton(resources.getString(R.string.dont_show)) { dialog, _ ->
                    viewModel.updateImportConfirm(true)
                    dialog.dismiss()
                }
            }
        }
        alertDialog.show()
    }

    private fun showRateDialog(): AlertDialog =
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
                            if (it.isSuccessful.not()) {
                                viewModel.resetRateUs()
                            }
                            dialog.dismiss()
                        }
                    } else {
                        navigateToPlayStore(task.exception?.message!!)
                        dialog.dismiss()
                    }
                }
                viewModel.neverShowRateUs()
            }
            .setNeutralButton(resources.getString(R.string.not_now)) { dialog, _ ->
                viewModel.resetRateUs()
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.never)) { dialog, _ ->
                viewModel.neverShowRateUs()
                dialog.cancel()
            }
            .setCancelable(false)
            .show()

    private fun showExitDialog(): AlertDialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.you_sure_quit))
            .setPositiveButton(resources.getString(R.string.exit)) { _, _ ->
                requireActivity().finish()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .show()

    private fun showEmailNotConfirmedDialog(): AlertDialog =
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

    private fun showErrorMaxTaskDialog(): AlertDialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.reaching_max_tasks))
            .setNeutralButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()

    private fun showErrorCheckingTaskDialog(): AlertDialog =
        showErrorCheckInternetConnectionDialog(
            requireContext(),
            resources.getString(R.string.error_checking_tasks),
            { importServerTasks() }
        )

    private fun showErrorImportTaskDialog(): AlertDialog =
        showErrorCheckInternetConnectionDialog(
            requireContext(),
            resources.getString(R.string.error_importing_tasks),
            { downloadServerList() }
        )
}