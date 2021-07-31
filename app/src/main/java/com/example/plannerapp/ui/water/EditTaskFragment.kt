package com.example.plannerapp.ui.water

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.plannerapp.R
import com.example.plannerapp.data.TaskType
import com.example.plannerapp.databinding.FragmentEditTaskBinding
import com.example.plannerapp.model.WaterSharedViewModel
import com.example.plannerapp.utils.GLOBAL_DATE
import com.example.plannerapp.utils.GLOBAL_DATE_FOR_CHECK
import com.example.plannerapp.utils.hideKeyboard
import com.example.plannerapp.utils.themeColor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class EditTaskFragment : Fragment() {
    private lateinit var task: TaskType
    private val navigationArgs: EditTaskFragmentArgs by navArgs()

    private var _binding: FragmentEditTaskBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WaterSharedViewModel by viewModels()


    private var bufferTimeValue: Int = 0
    private var bufferNameTask: String = ""
    private var bufferDescriptionTask: String = ""
    private var bufferPriorityTask: Int = 1

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
        val id = navigationArgs.idTask
        viewModel.retrieveTask(id).observe(this.viewLifecycleOwner) { selectedItem ->
            task = selectedItem
            bind(task)
        }
    }


    @SuppressLint("SimpleDateFormat")
    private fun bind(taskType: TaskType) {
        val items: Array<String> = resources.getStringArray(R.array.priorities)
        bufferTimeValue = task.timeTask
        bufferNameTask = task.nameTask.toString()
        bufferDescriptionTask = task.descriptionTask.toString()
        bufferPriorityTask = task.priorityTask

        binding.apply {
            tvCreatedTask.text = getString(R.string.task_created,taskType.createdTimeFormatted.toString())
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
            fragmentEditSaveBtn.setOnClickListener { updateTask(task.idTask) }

            if(taskType.checkTask){
                fragmentEditNameTaskLayout.isEnabled = false

                fragmentEditDescTaskLayout.isEnabled = false

                fragmentEditTimeLayout.isEnabled = false

                textPriorityLayout.isEnabled = false

                fragmentEditSaveBtn.isEnabled = false
                fragmentEditSaveBtn.setBackgroundColor(Color.GRAY)
            }
        }

    }

    @SuppressLint("SimpleDateFormat")
    private fun cancelEdit() {
        val items: Array<String> = resources.getStringArray(R.array.priorities)
        if (binding.fragmentEditNameTask.text.toString() != bufferNameTask ||
            binding.fragmentEditDescTask.text.toString() != bufferDescriptionTask ||
            task.timeTask.toString() != bufferTimeValue.toString() ||
            task.priorityTask != bufferPriorityTask
        ) {
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


    private fun updateTask(id: Int) {
        viewModel.updateTask(
            id,
            binding.fragmentEditNameTask.text.toString(),
            binding.fragmentEditDescTask.text.toString(),
            bufferTimeValue,
            bufferPriorityTask,
            task.checkTask
        )
        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT)
            .show()
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


    //navigateUp custom behavior
    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(menuItem)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideKeyboard(requireActivity())
        _binding = null
    }
}
