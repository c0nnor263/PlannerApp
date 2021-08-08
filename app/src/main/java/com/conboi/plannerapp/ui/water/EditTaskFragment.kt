package com.example.plannerapp.ui.water

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.plannerapp.R
import com.example.plannerapp.data.TaskType
import com.example.plannerapp.databinding.FragmentEditTaskBinding
import com.example.plannerapp.utils.GLOBAL_DATE
import com.example.plannerapp.utils.GLOBAL_DATE_FOR_CHECK
import com.example.plannerapp.utils.themeColor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class EditTaskFragment : Fragment() {
    private var _binding: FragmentEditTaskBinding? = null
    private val binding get() = _binding!!

    private val navigationArgs: EditTaskFragmentArgs by navArgs()
    private val viewModel: WaterSharedViewModel by viewModels()
    private lateinit var task: TaskType

    private var bufferTimeValue: Int = 0
    private var bufferNameTask: String = ""
    private var bufferDescriptionTask: String = ""
    private var bufferPriorityTask: Int = 1
    private lateinit var bufferMenu: Menu

    override fun onResume() {
        super.onResume()
        val items: Array<String> = resources.getStringArray(R.array.priorities)
        val adapter = ArrayAdapter(requireContext(), R.layout.list_priority_item_edit_task, items)
        binding.apply {
            dropPriority.setAdapter(adapter)
            dropPriority.setOnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> {
                        bufferPriorityTask = 0
                        dropPriority.setBackgroundResource(R.drawable.gradient_priority_leisurely)
                    }
                    1 -> {
                        bufferPriorityTask = 1
                        dropPriority.setBackgroundResource(R.drawable.gradient_priority_default)
                    }
                    2 -> {
                        bufferPriorityTask = 2
                        dropPriority.setBackgroundResource(R.drawable.gradient_priority_advisable)
                    }
                    3 -> {
                        bufferPriorityTask = 3
                        dropPriority.setBackgroundResource(R.drawable.gradient_priority_important)
                    }
                    else -> {
                        bufferPriorityTask = 1
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.navigation_host
            duration = 300
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().themeColor(R.attr.colorSurface))
        }
        setHasOptionsMenu(true)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            this.isEnabled = true
            cancelEdit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        requireActivity().window.navigationBarColor = Color.TRANSPARENT
        val id = navigationArgs.idTask
        viewModel.retrieveTask(id).observe(this.viewLifecycleOwner) { selectedItem ->
            task = selectedItem
            bufferNameTask = task.nameTask.toString()
            bufferDescriptionTask = task.descriptionTask.toString()
            bufferTimeValue = task.timeTask
            bufferPriorityTask = task.priorityTask
            bind(task)
        }
    }


    @SuppressLint("SimpleDateFormat")
    private fun bind(taskType: TaskType) {
        val items: Array<String> = resources.getStringArray(R.array.priorities)
        binding.apply {
            //TODO(Notifications)
            fragmentEditTimeInputText.setText(resources.getString(R.string.currently_unavailable))
            fragmentEditTimeLayout.isEnabled = false


            tvCreatedTask.text = getString(R.string.task_created, taskType.createdTimeFormatted)
            tvCompletedTask.isVisible = false

            fragmentEditNameTask.setText(taskType.nameTask)
            fragmentEditDescTask.setText(taskType.descriptionTask)
            if (task.timeTask != GLOBAL_DATE_FOR_CHECK) {
                fragmentEditTimeInputText.setText(
                    SimpleDateFormat(
                        "h:mm a"
                    ).format(
                        Date(task.timeTask.toLong() * 1000)
                    )
                )
            }
            fragmentEditDescTask.doOnTextChanged { _, _, _, _ ->
                if (fragmentEditDescTask.length() > 70) {
                    fragmentEditDescTaskLayout.error =
                        requireView().resources.getString(R.string.error_input_desc)
                } else {
                    fragmentEditDescTaskLayout.error = null
                }
            }

            fragmentEditTimeInputText.setOnClickListener { setTime() }
            when (taskType.priorityTask) {
                0 -> {
                    dropPriority.setText(items[0])
                    dropPriority.setBackgroundResource(R.drawable.gradient_priority_leisurely)
                }
                1 -> {
                    dropPriority.setText(items[1])
                    dropPriority.setBackgroundResource(R.drawable.gradient_priority_default)
                }
                2 -> {
                    dropPriority.setText(items[2])
                    dropPriority.setBackgroundResource(R.drawable.gradient_priority_advisable)
                }
                3 -> {
                    dropPriority.setText(items[3])
                    dropPriority.setBackgroundResource(R.drawable.gradient_priority_important)
                }
                else -> {
                    dropPriority.setText(items[1])
                    dropPriority.setBackgroundResource(R.drawable.gradient_priority_default)
                }
            }

            if (taskType.checkTask) {
                fragmentEditNameTaskLayout.isEnabled = false

                fragmentEditDescTaskLayout.isEnabled = false

                fragmentEditTimeLayout.isEnabled = false

                textPriorityLayout.isEnabled = false

                tvCompletedTask.isVisible = true
                tvCompletedTask.text =
                    getString(R.string.task_completed, taskType.completedTimeFormatted)

            }
            bufferMenu.findItem(R.id.save_btn).isEnabled = !taskType.checkTask
            bufferMenu.findItem(R.id.save_btn).isVisible = !taskType.checkTask

        }

    }

    @SuppressLint("SimpleDateFormat")
    private fun cancelEdit() {

        if (binding.fragmentEditNameTask.text.toString() != bufferNameTask ||
            binding.fragmentEditDescTask.text.toString() != bufferDescriptionTask ||
            task.timeTask.toString() != bufferTimeValue.toString() ||
            task.priorityTask != bufferPriorityTask
        ) {
            val items: Array<String> = resources.getStringArray(R.array.priorities)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Warning")
                .setMessage("Are you sure want to exit without changes?")
                .setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    findNavController().navigateUp()
                    binding.apply {
                        fragmentEditNameTask.setText(bufferNameTask)
                        fragmentEditDescTask.setText(bufferDescriptionTask)
                        fragmentEditTimeInputText.setText(
                            SimpleDateFormat(
                                "h:mm a"
                            ).format(
                                Date(task.timeTask.toLong() * 1000)
                            )
                        )
                        dropPriority.setText(items[bufferPriorityTask])
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        } else {
            findNavController().navigateUp()
        }
    }


    private fun updateTask(taskType: TaskType) {
        if (binding.fragmentEditNameTask.text.toString() != bufferNameTask ||
            binding.fragmentEditDescTask.text.toString() != bufferDescriptionTask ||
            task.timeTask.toString() != bufferTimeValue.toString() ||
            task.priorityTask != bufferPriorityTask) {
            binding.apply {
                viewModel.updateTask(
                    taskType,
                    fragmentEditNameTask.text.toString(),
                    fragmentEditDescTask.text.toString(),
                    bufferTimeValue,
                    bufferPriorityTask
                )
            }
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT)
                .show()
        }
        findNavController().navigateUp()
    }

    @SuppressLint("SimpleDateFormat")
    private fun setTime() {
        val calendar: Calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(context, { _, hourOfDay, minute ->
            bufferTimeValue = GLOBAL_DATE + (hourOfDay - 3) * 3600 + minute * 60
            binding.fragmentEditTimeInputText.setText(
                SimpleDateFormat(
                    "h:mm a"
                ).format(
                    Date((bufferTimeValue).toLong() * 1000)
                )
            )
        }, currentHour, currentMinute, false)
        timePickerDialog.show()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_edit_task_menu, menu)
        bufferMenu = menu
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                return true
            }
            R.id.save_btn -> {
                updateTask(task)
                return true
            }

        }
        return super.onOptionsItemSelected(menuItem)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}
