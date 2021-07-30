package com.example.plannerapp.ui.water

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plannerapp.R
import com.example.plannerapp.adapter.TaskAdapter
import com.example.plannerapp.data.SortOrder
import com.example.plannerapp.data.TaskType
import com.example.plannerapp.databinding.FragmentWaterBinding
import com.example.plannerapp.model.WaterSharedViewModel
import com.example.plannerapp.utils.GLOBAL_DATE_FOR_CHECK
import com.example.plannerapp.utils.hideKeyboard
import com.example.plannerapp.utils.onQueryTextChanged
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*


@AndroidEntryPoint
class WaterFragment : Fragment(R.layout.fragment_water), TaskAdapter.OnItemClickListener {
    private val viewModel: WaterSharedViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    lateinit var task: TaskType

    //FABs
    private var mLastClickTime: Long = 0

    //WaterFragment variables
    private var enteredCount: Int? = null
    private var sizeOfTasks: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Material animation
        exitTransition = MaterialElevationScale(false).apply {
            duration = 300
        }
        reenterTransition = MaterialElevationScale(false).apply {
            duration = 300
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setTitle("Are you sure you want to quit?")
            builder.setPositiveButton("Exit") { _, _ ->
                requireActivity().finish()
            }
            builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            builder.show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWaterBinding.bind(view)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        view.background.alpha = 60

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            recyclerView = recyclerViewTasks
        }

        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val mAdapter = TaskAdapter(this)
        recyclerView.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)

            setHasFixedSize(true)

            //Swipe delete task
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    task = mAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.swipeDeleteTask(task)
                }
            }).attachToRecyclerView(this)
        }

        viewModel.apply {
            //LiveData size of tasks list
            allTasksSize.observe(this@WaterFragment.viewLifecycleOwner) {
                sizeOfTasks = it
            }

            //LiveData list of tasks
            allTasks.observe(this@WaterFragment.viewLifecycleOwner) { items ->
                items.let {
                    mAdapter.submitList(it)
                }
            }

        }


        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.taskEvent.collect { event ->
                when (event) {
                    is WaterSharedViewModel.TasksEvent.ShowUndoDeleteTaskMessage -> {
                        val snackBar =
                            Snackbar.make(
                                view.findViewById(R.id.fragment_water_parent_layout),
                                "Task deleted",
                                Snackbar.LENGTH_LONG
                            )
                        snackBar.setAction("UNDO") {
                            viewModel.onUndoDeleteClick(event.task)
                        }
                        snackBar.show()
                    }
                }

            }
        }
        setHasOptionsMenu(true)
        enteredCount = 0

        findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.bottom_menu_water_fragment -> {
                    bottomNav.visibility = View.VISIBLE
                    bottomNav.menu.findItem(R.id.bottom_menu_water_fragment)
                        .setOnMenuItemClickListener {
                            preventOnClick()
                            if (it.isChecked) {
                                val popupMenu = PopupMenu(bottomNav.context, bottomNav)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    popupMenu.setForceShowIcon(true)
                                }
                                popupMenu.inflate(R.menu.bottom_navigation_water_item_menu)
                                popupMenu.setOnMenuItemClickListener { popupMenuItem ->
                                    when (popupMenuItem.itemId) {
                                        R.id.add_amount_tasks -> {
                                            preventOnClick()
                                            addingMultipleTasks()
                                            true
                                        }
                                        R.id.add_task -> {
                                            preventOnClick()
                                            addingTask()
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                popupMenu.menu.findItem(R.id.count_of_tasks).title = getString(
                                    R.string.amount_of_tasks,
                                    sizeOfTasks,
                                    viewModel.maxTasksCount
                                )
                                popupMenu.show()
                            }
                            bottomNav.performClick()
                        }
                }
            }

        }
    }

    override fun onItemClick(taskType: TaskType) {
        viewModel.onTaskSelected(taskType)
    }

    override fun onCheckBoxClick(taskType: TaskType, isChecked: Boolean) {
        viewModel.onTaskCheckedChanged(taskType, isChecked)
    }

    override fun onNameChanged(taskType: TaskType, name: String) {
        viewModel.onNameChanged(taskType, name)
    }


    private fun preventOnClick() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 250) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
    }


    private fun addingTask() {
        if (viewModel.isListNotFull()) {
            preventOnClick()
            viewModel.addNewTask(
                "",
                "",
                GLOBAL_DATE_FOR_CHECK,
                1,
                false
            )
        } else {
            showingDialogMaximumTasks()
        }
    }

    private fun addingMultipleTasks() {
        preventOnClick()
        val viewInflated: View = LayoutInflater.from(context)
            .inflate(R.layout.enter_amount_of_tasks, view as ViewGroup?, false)
        val enterAmount = viewInflated.findViewById<View>(R.id.input_amount) as EditText
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Enter an amount of tasks")
        builder.setView(viewInflated)
        builder.setPositiveButton(R.string.add_task) { _, _ ->
            if (enterAmount.text.toString() != "") {
                enteredCount = enterAmount.text.toString().toInt()
            }
            if (viewModel.isListNotFull()) {
                if (enteredCount!! in 1..viewModel.maxTasksCount) {

                    if (enteredCount!!.plus(sizeOfTasks) > viewModel.maxTasksCount) {
                        enteredCount = viewModel.maxTasksCount - sizeOfTasks
                    }
                    viewModel.addNewTasks(
                        "",
                        "",
                        GLOBAL_DATE_FOR_CHECK,
                        1,
                        false,
                        enteredCount!!
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
        builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showingDialogMaximumTasks() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("You have reached your maximum of tasks")
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragments_water, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.onQueryTextChanged {
            viewModel.searchQuery.value = it
        }
        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                viewModel.preferencesFlow.first().hideCompleted
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_by_name -> {
                viewModel.onSortOrderSelected(SortOrder.BY_NAME)
                true
            }
            R.id.action_sort_by_date_created -> {
                viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                true
            }
            R.id.action_hide_completed_tasks -> {
                item.isChecked = !item.isChecked
                viewModel.onHideCompletedClick(item.isChecked)
                true
            }
            R.id.action_delete_completed_tasks -> {
                viewModel.deleteCompletedTasks()
                true
            }
            R.id.action_delete_all_tasks -> {
                viewModel.deleteAllData()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
    }


}

