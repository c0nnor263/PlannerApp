package com.conboi.plannerapp.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.SortOrder
import com.conboi.plannerapp.databinding.FragmentBottomActionsBinding
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class BottomActionsFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentBottomActionsBinding? = null
    val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_bottom_actions, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            lifecycleOwner = this@BottomActionsFragment
            when (arguments?.getInt("action")) {
                0 -> {
                    bottomActions.inflateMenu(R.menu.bottom_app_bar_main_delete_menu)
                    bottomActionsTitle.text = resources.getString(R.string.action_delete_tasks)
                }
                1 -> {
                    bottomActions.inflateMenu(R.menu.bottom_app_bar_main_sort_menu)
                    bottomActionsTitle.text = resources.getString(R.string.sort)
                }
            }
            bottomActions.setNavigationItemSelectedListener {
                when (it.itemId) {
                    //Delete
                    R.id.delete_only_completed_tasks -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(resources.getString(R.string.confirm_deletion))
                            .setMessage(resources.getString(R.string.you_delete_completed))
                            .setPositiveButton(resources.getString(R.string.confirm)) { _, _ ->
                                sharedViewModel.deleteOnlyCompletedTasks()
                                Toast.makeText(
                                    requireContext(),
                                    resources.getString(R.string.completed_deleted),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
                            .show()
                    }
                    R.id.delete_overcompleted_tasks -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(resources.getString(R.string.confirm_deletion))
                            .setMessage(resources.getString(R.string.you_delete_overcompleted))
                            .setPositiveButton(resources.getString(R.string.confirm)) { _, _ ->
                                sharedViewModel.deleteOnlyOvercompletedTasks()
                                Toast.makeText(
                                    requireContext(),
                                    resources.getString(R.string.overcompleted_deleted),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
                            .show()
                    }

                    //Sort
                    R.id.sort_by_title -> {
                        sharedViewModel.onSortOrderSelected(SortOrder.BY_TITLE)
                    }
                    R.id.sort_by_date_created -> {
                        sharedViewModel.onSortOrderSelected(SortOrder.BY_DATE)
                    }
                    R.id.sort_by_date_completed -> {
                        sharedViewModel.onSortOrderSelected(SortOrder.BY_COMPLETE)
                    }
                    R.id.sort_by_overcompleted -> {
                        sharedViewModel.onSortOrderSelected(SortOrder.BY_OVERCOMPLETED)
                    }
                }
                dismiss()
                true
            }

            bottomActions.apply {
                if (menu.findItem(R.id.delete_only_completed_tasks) != null) {
                    sharedViewModel.allOnlyCompletedTasksSize.observe(viewLifecycleOwner) {
                        menu.findItem(R.id.delete_only_completed_tasks).isEnabled = it > 0
                        //menu.findItem(R.id.sort_by_date_completed).isEnabled = it > 0
                    }
                }
                if (menu.findItem(R.id.delete_overcompleted_tasks) != null) {
                    sharedViewModel.allOnlyOverCompletedTasksSize.observe(viewLifecycleOwner) {
                        menu.findItem(R.id.delete_overcompleted_tasks).isEnabled = it > 0
                        // menu.findItem(R.id.sort_by_overcompleted).isEnabled = it > 0
                    }
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}