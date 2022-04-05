package com.conboi.plannerapp.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentBottomActionsBinding
import com.conboi.plannerapp.utils.BottomAction
import com.conboi.plannerapp.utils.SortOrder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class BottomActionFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentBottomActionsBinding? = null
    val binding get() = _binding!!

    private val viewModel: BottomActionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomActionsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (arguments?.getString("action")) {
            BottomAction.DELETE.name -> {
                binding.nvBottomActions.inflateMenu(R.menu.b_app_bar_main_delete_menu)
            }
            BottomAction.SORT.name -> {
                binding.nvBottomActions.inflateMenu(R.menu.b_app_bar_main_sort_menu)
            }
        }

        binding.nvBottomActions.setNavigationItemSelectedListener {
            when (it.itemId) {
                //Delete
                R.id.action_delete_only_completed_tasks -> deleteOnlyCompletedDialog()
                R.id.action_delete_overcompleted_tasks -> deleteOvercompletedDialog()

                //Sort
                R.id.sort_by_title -> viewModel.onSortOrderSelected(SortOrder.BY_TITLE)
                R.id.sort_by_date_created -> viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                R.id.sort_by_date_completed -> viewModel.onSortOrderSelected(SortOrder.BY_COMPLETE)
                R.id.sort_by_overcompleted -> viewModel.onSortOrderSelected(SortOrder.BY_OVERCOMPLETED)
            }
            dismiss()
            true
        }

        val menu = binding.nvBottomActions.menu
        if (menu.findItem(R.id.action_delete_only_completed_tasks) != null) {
            viewModel.completedTasksSize.observe(viewLifecycleOwner) {
                menu.findItem(R.id.action_delete_only_completed_tasks).isEnabled = it > 0
            }
        }
        if (menu.findItem(R.id.action_delete_overcompleted_tasks) != null) {
            viewModel.overcompletedTasksSize.observe(viewLifecycleOwner) {
                menu.findItem(R.id.action_delete_overcompleted_tasks).isEnabled = it > 0
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun deleteOvercompletedDialog(): AlertDialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.confirm_deletion))
            .setMessage(resources.getString(R.string.you_delete_overcompleted))
            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                viewModel.deleteOvercompletedTasks(requireContext())
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.overcompleted_deleted),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .show()


    private fun deleteOnlyCompletedDialog(): AlertDialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.confirm_deletion))
            .setMessage(resources.getString(R.string.you_delete_completed))
            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                viewModel.deleteCompletedTasks(requireContext())
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.completed_deleted),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .show()
}