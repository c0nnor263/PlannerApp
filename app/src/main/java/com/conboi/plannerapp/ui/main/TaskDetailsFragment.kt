package com.conboi.plannerapp.ui.main

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.os.VibrationEffect
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.AlertdialogAddDeadlineTimeBinding
import com.conboi.plannerapp.databinding.AlertdialogAddReminderTimeBinding
import com.conboi.plannerapp.databinding.FragmentTaskDetailsBinding
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.cancelReminder
import com.conboi.plannerapp.utils.setOrUpdateReminder
import com.conboi.plannerapp.utils.themeColor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class TaskDetailsFragment : Fragment() {
    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding get() = _binding!!

    private val navigationArgs: TaskDetailsFragmentArgs by navArgs()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val taskDetailsViewModel: TaskDetailsViewModel by viewModels()

    private var alarmManager: AlarmManager? = null
    private lateinit var bufferedTask: TaskType


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.navigation_host
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().themeColor(com.google.android.material.R.attr.colorSurface))
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
        (activity as MainActivity).binding.bottomFloatingButton.apply {
            setOnClickListener {
                updateTask()
            }
            setOnLongClickListener(null)
        }
        alarmManager = getSystemService(requireContext(), AlarmManager::class.java)
        binding.apply {
            lifecycleOwner = this@TaskDetailsFragment
            viewModel = taskDetailsViewModel
            taskDetailsFragment = this@TaskDetailsFragment
            toolbar.setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            taskDetailsViewModel.apply {
                sharedViewModel.getTask(navigationArgs.idTask)
                    .observe(this@TaskDetailsFragment.viewLifecycleOwner) { selectedItem ->
                        checkTask.isChecked = selectedItem.checked
                        setBufferTask(selectedItem)
                        updateTimeValue(selectedItem.time)
                        updateTotalCheckedValue(selectedItem.totalChecked)
                        updateCheckedValue(selectedItem.checked)
                        updateDeadlineValue(selectedItem.deadline)
                        updatePriorityValue(selectedItem.priority)
                        updateRepeatModeValue(selectedItem.repeatMode)
                        updateCompletedValue(selectedItem.completed, selectedItem.checked)
                        bufferedTask = selectedItem
                        getAndSetPriority()
                    }
                newChecked.observe(this@TaskDetailsFragment.viewLifecycleOwner) {
                    checkTask.isChecked = it
                    if (it) {
                        tvCompletedTask.visibility = View.VISIBLE
                    } else {
                        titleLayout.isEnabled = true
                        descLayout.isEnabled = true
                        timeLayout.isEnabled = true
                        deadlineLayout.isEnabled = true
                        priorityLayout.isEnabled = true
                        tvCompletedTask.visibility = View.GONE
                    }
                }
                newTotalChecked.observe(this@TaskDetailsFragment.viewLifecycleOwner) {
                    if (it > 1) {
                        titleLayout.isEnabled = true
                        descLayout.isEnabled = true
                        timeLayout.isEnabled = true
                        deadlineLayout.isEnabled = true
                        priorityLayout.isEnabled = true
                    } else {
                        titleLayout.isEnabled = false
                        descLayout.isEnabled = false
                        timeLayout.isEnabled = false
                        deadlineLayout.isEnabled = false
                        priorityLayout.isEnabled = false
                    }
                }


                viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                    if (!sharedViewModel.getRemindersState()) {

                        timeLayout.isEnabled = false
                        time.setText(resources.getString(R.string.reminders_are_off))
                        time.setOnClickListener(null)

                        deadlineLayout.isEnabled = false
                        deadline.setText(resources.getString(R.string.reminders_are_off))
                        deadline.setOnClickListener(null)
                    }
                }

                checkTask.setOnClickListener {
                    if (title.text.toString().isNotBlank()) {
                        if (newChecked.value!!) {
                            increaseTotalChecked()
                        } else {
                            decreaseTotalChecked()
                        }
                    } else {
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(resources.getString(R.string.cant_check_task))
                            .setNeutralButton(resources.getString(R.string.ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                        checkTask.isChecked = false
                        updateCheckedValue(false)
                    }
                }
                checkTask.setOnLongClickListener {
                    (activity as MainActivity).apply {
                        vb?.vibrate(
                            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                        vb?.vibrate(
                            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    }
                    if (title.text.toString().isNotBlank()) {
                        updateCheckedValue(true)
                        increaseTotalChecked()
                    } else {
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(resources.getString(R.string.cant_check_task))
                            .setNeutralButton(resources.getString(R.string.ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                        checkTask.isChecked = false
                        updateCheckedValue(false)
                    }
                    true
                }
            }
        }
    }

    fun getAndSetPriority() {
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
                        dropPriority.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.secondaryDarkColorFire
                            )
                        )
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
        var reminderBinding: AlertdialogAddReminderTimeBinding? =
            DataBindingUtil.inflate(
                layoutInflater,
                R.layout.alertdialog_add_reminder_time,
                view as ViewGroup?,
                false
            )

        var setTime: Calendar = Calendar.getInstance().apply {
            timeInMillis = GLOBAL_START_DATE
        }
        if (taskDetailsViewModel.bufferTask.value!!.time != GLOBAL_START_DATE) {
            setTime.timeInMillis = taskDetailsViewModel.bufferTask.value!!.time
        }
        setTime.set(
            Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR)
        )
        setTime.set(
            Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH)
        )
        setTime.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

        val addReminderDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.add_reminder))
            .setView(reminderBinding!!.root)
            .setCancelable(false)
            .setPositiveButton(resources.getString(R.string.save)) { dialog, _ ->
                if (taskDetailsViewModel.newTime.value != GLOBAL_START_DATE &&
                    taskDetailsViewModel.newTime.value != taskDetailsViewModel.bufferTask.value!!.time
                ) {
                    var remainingTime: Long
                    var minutes = 0L
                    var hours = 0L
                    var days = 0L

                    fun updateRemainingTime() {
                        remainingTime =
                            setTime.timeInMillis - Calendar.getInstance().timeInMillis
                        minutes = (remainingTime / (1000 * 60) % 60)
                        hours = (remainingTime / (1000 * 60 * 60) % 24)
                        days = TimeUnit.MILLISECONDS.toDays(remainingTime)
                    }
                    updateRemainingTime()

                    if ((minutes < 0L || hours < 0L || days < 0L)
                    ) {
                        setTime.add(Calendar.DATE, 1)
                        updateRemainingTime()
                        taskDetailsViewModel.updateTimeValue(setTime.timeInMillis)
                    }

                    when {
                        days != 0L -> {
                            Toast.makeText(
                                context,
                                "Reminder set for $days days, $hours hours and $minutes minutes from now.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        hours != 0L -> {
                            Toast.makeText(
                                context,
                                "Reminder set for $hours hours and $minutes minutes from now.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        hours == 0L -> {
                            if (minutes == 0L) {
                                Toast.makeText(
                                    context,
                                    "Reminder set for less than minute from now.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Reminder set for $minutes minutes from now.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        else -> {
                            Toast.makeText(
                                context,
                                "Reminder set for $days days, $hours hours and $minutes minutes from now.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    binding.time.setText(
                        SimpleDateFormat(
                            "EEE, d MMM, h:mm a", Locale.getDefault()
                        ).format(
                            Date(setTime.timeInMillis)
                        )
                    )
                } else {
                    binding.time.setText(
                        resources.getString(R.string.set_reminder)
                    )
                }

                reminderBinding = null
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel))
            { dialog, _ ->
                reminderBinding = null
                taskDetailsViewModel.updateRepeatModeValue(taskDetailsViewModel.bufferTask.value!!.repeatMode)
                dialog.cancel()
            }
            .show()

        reminderBinding?.apply {
            lifecycleOwner = this@TaskDetailsFragment
            viewModel = taskDetailsViewModel

            if (taskDetailsViewModel.newTime.value!! == GLOBAL_START_DATE) {
                removeReminder.visibility = View.GONE
            }

            openTimepicker.setOnClickListener {
                setTime = openTimePicker(null, reminderBinding, setTime)
            }
            openDatepicker.setOnClickListener {
                setTime = openDatePicker(null, reminderBinding, setTime)
            }

            getAndSetRepeatMode(reminderBinding!!)

            removeReminder.setOnClickListener {
                taskDetailsViewModel.updateTimeValue(GLOBAL_START_DATE)
                taskDetailsViewModel.updateRepeatModeValue(0)
                binding.time.setText(resources.getString(R.string.set_reminder))
                addReminderDialog.cancel()
                Toast.makeText(
                    context,
                    resources.getString(R.string.reminder_task_removed),
                    Toast.LENGTH_SHORT
                )
                    .show()
                reminderBinding = null
                reminderAddTimeEdittext.setText(resources.getString(R.string.time_word))
                reminderAddDateEdittext.setText(resources.getString(R.string.date_word))
            }
        }

    }

    fun setDeadline() {
        var deadlineBinding: AlertdialogAddDeadlineTimeBinding? =
            DataBindingUtil.inflate(
                layoutInflater,
                R.layout.alertdialog_add_deadline_time,
                view as ViewGroup?,
                false
            )

        var setTime: Calendar = Calendar.getInstance().apply {
            timeInMillis = GLOBAL_START_DATE
        }
        if (taskDetailsViewModel.bufferTask.value!!.deadline != GLOBAL_START_DATE) {
            setTime.timeInMillis = taskDetailsViewModel.bufferTask.value!!.deadline
        }
        setTime.set(
            Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR)
        )
        setTime.set(
            Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH)
        )
        setTime.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

        val addDeadlineDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.add_deadline))
            .setView(deadlineBinding!!.root)
            .setCancelable(false)
            .setPositiveButton(resources.getString(R.string.save)) { dialog, _ ->
                if (taskDetailsViewModel.newDeadline.value != GLOBAL_START_DATE) {
                    var remainingTime: Long
                    var minutes = 0L
                    var hours = 0L
                    var days = 0L

                    fun updateRemainingTime() {
                        remainingTime =
                            setTime.timeInMillis - Calendar.getInstance().timeInMillis
                        minutes = (remainingTime / (1000 * 60) % 60)
                        hours = (remainingTime / (1000 * 60 * 60) % 24)
                        days = TimeUnit.MILLISECONDS.toDays(remainingTime)
                    }
                    updateRemainingTime()

                    if (minutes < 0L || hours < 0L || days < 0L) {
                        setTime.add(Calendar.DATE, 1)
                        updateRemainingTime()
                        taskDetailsViewModel.updateDeadlineValue(setTime.timeInMillis)
                    }

                    when {
                        days != 0L -> {
                            Toast.makeText(
                                context,
                                "Deadline set for $days days, $hours hours and $minutes minutes from now.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        hours != 0L -> {
                            Toast.makeText(
                                context,
                                "Deadline set for $hours hours and $minutes minutes from now.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        hours == 0L -> {
                            if (minutes == 0L) {
                                Toast.makeText(
                                    context,
                                    "Deadline set for less than minute from now.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Deadline set for $minutes minutes from now.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        else -> {
                            Toast.makeText(
                                context,
                                "Deadline set for $days days, $hours hours and $minutes minutes from now.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    binding.deadline.setText(
                        SimpleDateFormat(
                            "EEE, d MMM, h:mm a", Locale.getDefault()
                        ).format(
                            Date(setTime.timeInMillis)
                        )
                    )
                } else {
                    binding.deadline.setText(
                        resources.getString(R.string.set_deadline)
                    )
                }

                deadlineBinding = null
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel))
            { dialog, _ ->
                deadlineBinding = null
                dialog.cancel()
            }
            .show()

        deadlineBinding?.apply {
            lifecycleOwner = this@TaskDetailsFragment
            viewModel = taskDetailsViewModel

            if (taskDetailsViewModel.newDeadline.value!! == GLOBAL_START_DATE) {
                removeDeadline.visibility = View.GONE
            }

            openTimepicker.setOnClickListener {
                setTime = openTimePicker(deadlineBinding, null, setTime)
            }
            openDatepicker.setOnClickListener {
                setTime = openDatePicker(deadlineBinding, null, setTime)
            }

            removeDeadline.setOnClickListener {
                taskDetailsViewModel.updateDeadlineValue(GLOBAL_START_DATE)
                binding.time.setText(resources.getString(R.string.set_deadline))
                addDeadlineDialog.cancel()
                Toast.makeText(
                    context,
                    resources.getString(R.string.deadline_task_removed),
                    Toast.LENGTH_SHORT
                )
                    .show()
                deadlineBinding = null
                deadlineAddTimeEdittext.setText(resources.getString(R.string.time_word))
                deadlineAddDateEdittext.setText(resources.getString(R.string.date_word))
            }
        }
    }

    private fun setReminder(title: String, triggerTime: Long, repeatMode: Int) {
        lifecycleScope.launch {
            alarmManager!!.setOrUpdateReminder(
                requireContext(),
                bufferedTask.idTask,
                title,
                triggerTime,
                repeatMode
            )
        }
    }

    private fun cancelReminder() {
        alarmManager!!.cancelReminder(requireContext(), bufferedTask.idTask)
    }

    private fun getAndSetRepeatMode(reminderBinding: AlertdialogAddReminderTimeBinding) {
        reminderBinding.dropRepeatMode.setOnItemClickListener { _, _, position, _ ->
            taskDetailsViewModel.updateRepeatModeValue(position)
        }
    }

    private fun openTimePicker(
        deadlineTimeBinding: AlertdialogAddDeadlineTimeBinding?,
        reminderBinding: AlertdialogAddReminderTimeBinding?,
        calendar: Calendar
    ): Calendar {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)

                if (reminderBinding != null) {
                    reminderBinding.reminderAddTimeEdittext.setText(
                        SimpleDateFormat(
                            "h:mm a", Locale.getDefault()
                        ).format(
                            Date(calendar.timeInMillis)
                        )
                    )
                    taskDetailsViewModel.updateTimeValue(calendar.timeInMillis)
                } else {
                    deadlineTimeBinding!!.deadlineAddTimeEdittext.setText(
                        SimpleDateFormat(
                            "h:mm a", Locale.getDefault()
                        ).format(
                            Date(calendar.timeInMillis)
                        )
                    )
                    taskDetailsViewModel.updateDeadlineValue(calendar.timeInMillis)
                }
            },
            calendar.get(Calendar.HOUR),
            calendar.get(Calendar.MINUTE),
            DateFormat.is24HourFormat(requireActivity())
        ).show()
        return calendar
    }

    private fun openDatePicker(
        deadlineTimeBinding: AlertdialogAddDeadlineTimeBinding?,
        reminderBinding: AlertdialogAddReminderTimeBinding?,
        calendar: Calendar
    ): Calendar {

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                if (reminderBinding != null) {
                    reminderBinding.reminderAddDateEdittext.setText(
                        SimpleDateFormat(
                            "EEE, d MMM yyyy", Locale.getDefault()
                        ).format(
                            Date(calendar.timeInMillis)
                        )
                    )
                    taskDetailsViewModel.updateTimeValue(calendar.timeInMillis)
                } else {
                    deadlineTimeBinding!!.deadlineAddDateEdittext.setText(
                        SimpleDateFormat(
                            "EEE, d MMM yyyy", Locale.getDefault()
                        ).format(
                            Date(calendar.timeInMillis)
                        )
                    )
                    taskDetailsViewModel.updateDeadlineValue(calendar.timeInMillis)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
        return calendar
    }

    private fun isEdited(): Boolean {
        binding.apply {
            taskDetailsViewModel.apply {
                return title.text.toString() != bufferedTask.title ||
                        desc.text.toString() != bufferedTask.description ||
                        checkTask.isChecked != bufferedTask.checked ||
                        newTime.value != bufferedTask.time ||
                        newRepeatMode.value != bufferedTask.repeatMode ||
                        newDeadline.value != bufferedTask.deadline ||
                        newPriority.value != bufferedTask.priority ||
                        newCompleted.value != bufferedTask.completed ||
                        newTotalChecked.value != bufferedTask.totalChecked
            }
        }
    }

    private fun updateTask() {
        binding.apply {
            taskDetailsViewModel.apply {
                if (isEdited()) {
                    if (checkTask.isChecked != bufferedTask.checked) {
                        sharedViewModel.onTaskCheckedChanged(
                            bufferedTask,
                            checkTask.isChecked,
                            false
                        )
                    }
                    if (newTotalChecked.value != bufferedTask.totalChecked) {
                        val differ: Int
                        if (newTotalChecked.value!! > bufferedTask.totalChecked) {
                            differ = if (bufferedTask.totalChecked == 0) {
                                newTotalChecked.value!! - bufferedTask.totalChecked - 1
                            } else {
                                newTotalChecked.value!! - bufferedTask.totalChecked
                            }
                            sharedViewModel.incrementTotalCompleted(differ)

                        } else if (newTotalChecked.value!! < bufferedTask.totalChecked) {
                            differ = if (newTotalChecked.value!! == 0) {
                                bufferedTask.totalChecked - newTotalChecked.value!! - 1
                            } else {
                                bufferedTask.totalChecked - newTotalChecked.value!!
                            }
                            sharedViewModel.decrementTotalCompleted(differ)
                        }
                    }
                    sharedViewModel.updateTask(
                        bufferedTask,
                        title.text.toString(),
                        desc.text.toString(),
                        newTime.value!!,
                        newDeadline.value!!,
                        newPriority.value!!,
                        checkTask.isChecked,
                        newCompleted.value!!,
                        newRepeatMode.value!!,
                        newTotalChecked.value!!
                    )
                    if (newTime.value != bufferedTask.time) {
                        if (newTime.value != GLOBAL_START_DATE) {
                            setReminder(
                                title.text.toString(),
                                newTime.value!!,
                                newRepeatMode.value!!
                            )
                        } else {
                            cancelReminder()
                        }
                    }
                    Toast.makeText(
                        context,
                        resources.getString(R.string.saved),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
        findNavController().navigateUp()
    }

    private fun cancelEdit() {
        taskDetailsViewModel.apply {
            if (isEdited()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(resources.getString(R.string.warning))
                    .setMessage(resources.getString(R.string.you_quit_without_changes))
                    .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                        dialog.dismiss()
                        findNavController().navigateUp()
                    }
                    .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()
            } else {
                findNavController().navigateUp()
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.apply {
            taskDetailsViewModel.saveState(
                bufferedTask,
                title.text.toString(),
                desc.text.toString(),
                checkTask.isChecked
            )
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            val retrieveData = taskDetailsViewModel.retrieveState()
            binding.title.setText(retrieveData.first)
            binding.desc.setText(retrieveData.second)
            binding.checkTask.isChecked = retrieveData.third
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        _binding = null
        bufferedTask = TaskType()
    }
}
