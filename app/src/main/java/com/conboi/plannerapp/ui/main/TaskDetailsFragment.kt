package com.conboi.plannerapp.ui.main

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.utils.GLOBAL_DATE
import com.conboi.plannerapp.utils.themeColor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.*


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class TaskDetailsFragment : Fragment() {
    private var _binding: com.conboi.plannerapp.databinding.FragmentTaskDetailsBinding? = null
    private val binding get() = _binding!!

    private val navigationArgs: TaskDetailsFragmentArgs by navArgs()
    private val sharedViewModel: SharedViewModel by viewModels()
    private val taskDetailsViewModel: TaskDetailsViewModel by viewModels()

    private lateinit var bufferedTask: TaskType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.navigation_host
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().themeColor(R.attr.colorSurface))
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            this.isEnabled = true
            cancelEdit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_task_details, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().bottom_floating_button.apply {
            setOnClickListener {
                updateTask()
            }
            setOnLongClickListener(null)
        }

        binding.apply {
            lifecycleOwner = this@TaskDetailsFragment
            viewModel = taskDetailsViewModel
            editTaskFragment = this@TaskDetailsFragment
            fragmentTaskDetailsToolbar.setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            sharedViewModel.getTask(navigationArgs.idTask)
                .observe(this@TaskDetailsFragment.viewLifecycleOwner) { selectedItem ->
                    taskDetailsViewModel.apply {
                        fragmentEditCheckTask.isChecked = selectedItem.checked
                        setBufferTask(selectedItem)
                        updateTimeValue(selectedItem.time)
                        updatePriorityValue(selectedItem.priority)
                        updateCompletedValue(selectedItem.completed, selectedItem.checked)

                        bufferedTask = selectedItem
                        priorityList()
                    }
                }
        }
    }


    fun priorityList() {
        binding.apply {
            dropPriority.setOnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> {
                        taskDetailsViewModel.updatePriorityValue(0)
                        dropPriority.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryDarkColorAir
                            )
                        )
                    }
                    1 -> {
                        taskDetailsViewModel.updatePriorityValue(1)
                        dropPriority.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.secondaryColorWater
                            )
                        )
                    }
                    2 -> {
                        taskDetailsViewModel.updatePriorityValue(2)
                        dropPriority.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryLightColorFire
                            )
                        )
                    }
                    3 -> {
                        taskDetailsViewModel.updatePriorityValue(3)
                        dropPriority.setTextColor(Color.parseColor("#c62828"))
                    }
                    else -> {
                        taskDetailsViewModel.updatePriorityValue(1)
                        dropPriority.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryLightColorWater
                            )
                        )
                    }
                }
            }
        }
    }

    fun setTime() {
        val calendar: Calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(context, { _, hourOfDay, minute ->
            taskDetailsViewModel.updateTimeValue(GLOBAL_DATE + (hourOfDay - 3) * 3600 + minute * 60)
            binding.fragmentEditTimeInputText.setText(
                SimpleDateFormat(
                    "h:mm a", Locale.getDefault()
                ).format(
                    Date((taskDetailsViewModel.newTaskTime.value)!!.toLong() * 1000)
                )
            )
        }, currentHour, currentMinute, false)
        timePickerDialog.show()
    }

    private fun cancelEdit() {
        taskDetailsViewModel.apply {
            if (binding.fragmentEditTitleTask.text.toString() != bufferedTask.title ||
                binding.fragmentEditDescTask.text.toString() != bufferedTask.description ||
                binding.fragmentEditCheckTask.isChecked != bufferedTask.checked ||
                bufferedTask.time.toString() != newTaskTime.value.toString() ||
                bufferedTask.priority != newTaskPriority.value ||
                bufferedTask.completed != newTaskCompleted.value
            ) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Warning")
                    .setMessage("Are you sure want to exit without changes?")
                    .setPositiveButton("Yes") { dialog, _ ->
                        dialog.dismiss()
                        findNavController().navigateUp()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun updateTask() {
        binding.apply {
            taskDetailsViewModel.apply {
                if (fragmentEditTitleTask.text.toString() != bufferedTask.title ||
                    fragmentEditDescTask.text.toString() != bufferedTask.description ||
                    fragmentEditCheckTask.isChecked != bufferedTask.checked ||
                    newTaskTime.value.toString() != bufferedTask.time.toString() ||
                    newTaskPriority.value != bufferedTask.priority ||
                    newTaskCompleted.value != bufferedTask.completed
                ) {
                    if (fragmentEditCheckTask.isChecked != bufferedTask.checked) {
                        sharedViewModel.onTaskCheckedChanged(
                            bufferedTask,
                            fragmentEditCheckTask.isChecked
                        )
                    }
                    sharedViewModel.updateTask(
                        bufferedTask,
                        fragmentEditTitleTask.text.toString(),
                        fragmentEditDescTask.text.toString(),
                        newTaskTime.value!!,
                        newTaskPriority.value!!,
                        fragmentEditCheckTask.isChecked,
                        newTaskCompleted.value!!
                    )
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT)
                        .show()

                }
            }
        }
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        _binding = null
        bufferedTask = TaskType(title = "", description = "")
    }
}
