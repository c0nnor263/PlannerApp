package com.conboi.plannerapp.ui.main

import android.app.AlarmManager
import android.graphics.Color
import android.os.Bundle
import android.os.VibrationEffect
import android.text.format.DateFormat.is24HourFormat
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.work.*
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.AlertdialogAddDeadlineTimeBinding
import com.conboi.plannerapp.databinding.AlertdialogAddReminderTimeBinding
import com.conboi.plannerapp.databinding.FragmentTaskDetailsBinding
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.myclass.AlarmMethods
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class TaskDetailsFragment : Fragment() {
    @Inject
    lateinit var alarmMethods: AlarmMethods

    @Inject
    lateinit var mainFragment: MainFragment

    private var _binding: FragmentTaskDetailsBinding? = null
    private val binding get() = _binding!!

    private val navigationArgs: TaskDetailsFragmentArgs by navArgs()
    private val sharedViewModel: SharedViewModel by viewModels()
    private val taskDetailsViewModel: TaskDetailsViewModel by viewModels()

    private var bufferedTask: TaskType? = null

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
                        updateCompletedValue(selectedItem.completed)
                        updateMissedValue(selectedItem.missed)
                        updateLastOvercheck(selectedItem.lastOvercheck)
                        bufferedTask = selectedItem
                    }
                newChecked.observe(this@TaskDetailsFragment.viewLifecycleOwner) {
                    checkTask.isChecked = it
                    if (it) {
                        tvCompletedTask.visibility = View.VISIBLE
                        if (bufferTask.value?.missed == true) {
                            deadline.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    android.R.color.black
                                )
                            )
                        }
                    } else {
                        titleLayout.isEnabled = true
                        descLayout.isEnabled = true
                        timeLayout.isEnabled = true
                        deadlineLayout.isEnabled = true
                        priorityLayout.isEnabled = true
                        if (newTotalChecked.value!! == 0) {
                            tvCompletedTask.visibility = View.GONE
                        }
                        if (bufferTask.value?.missed == true) {
                            deadline.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.secondaryDarkColorFire
                                )
                            )
                        }
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
                    if (System.currentTimeMillis() - newLastOvercheck.value!! >= AlarmManager.INTERVAL_DAY) {
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
                    } else {
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(resources.getString(R.string.cant_overcheck_task))
                            .setNeutralButton(resources.getString(R.string.ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    true
                }
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
    }

    fun setTimeReminder() {
        var reminderBinding: AlertdialogAddReminderTimeBinding =
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
            .setView(reminderBinding.root)
            .setCancelable(false)
            .setPositiveButton(resources.getString(R.string.save)) { dialog, _ ->
                if (taskDetailsViewModel.newTime.value != GLOBAL_START_DATE
                ) {
                    if (taskDetailsViewModel.newTime.value != taskDetailsViewModel.bufferTask.value!!.time) {
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
                            days != 0.toLong() -> {
                                Toast.makeText(
                                    context,
                                    getString(
                                        R.string.reminder_for_day_hour_minute,
                                        resources.getQuantityString(
                                            R.plurals.days,
                                            days.toInt(),
                                            days
                                        ),
                                        resources.getQuantityString(
                                            R.plurals.hours,
                                            hours.toInt(),
                                            hours
                                        ),
                                        resources.getQuantityString(
                                            R.plurals.minutes,
                                            minutes.toInt(),
                                            minutes
                                        )
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            hours != 0.toLong() -> {
                                Toast.makeText(
                                    context,
                                    getString(
                                        R.string.reminder_for_hour_minute,
                                        resources.getQuantityString(
                                            R.plurals.hours,
                                            hours.toInt(),
                                            hours
                                        ),
                                        resources.getQuantityString(
                                            R.plurals.minutes,
                                            minutes.toInt(),
                                            minutes
                                        )
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            hours == 0.toLong() -> {
                                if (minutes == 0.toLong()) {
                                    Toast.makeText(
                                        context,
                                        getString(R.string.reminder_less_than_minute),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        getString(
                                            R.string.reminder_for_minutes,
                                            resources.getQuantityString(
                                                R.plurals.minutes,
                                                minutes.toInt(),
                                                minutes
                                            )
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            else -> {
                                Toast.makeText(
                                    context,
                                    getString(
                                        R.string.reminder_for_day_hour_minute,
                                        resources.getQuantityString(
                                            R.plurals.days,
                                            days.toInt(),
                                            days
                                        ),
                                        resources.getQuantityString(
                                            R.plurals.hours,
                                            hours.toInt(),
                                            hours
                                        ),
                                        resources.getQuantityString(
                                            R.plurals.minutes,
                                            minutes.toInt(),
                                            minutes
                                        )
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        binding.time.setText(
                            DateFormat.getDateTimeInstance(
                                DateFormat.DEFAULT,
                                DateFormat.DEFAULT,
                                resources.configuration.locales[0]
                            )
                                .format(
                                    Date(setTime.timeInMillis)
                                )
                        )
                    }
                } else {
                    binding.time.setText(
                        resources.getString(R.string.set_reminder)
                    )
                }
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel))
            { dialog, _ ->
                taskDetailsViewModel.updateRepeatModeValue(taskDetailsViewModel.bufferTask.value!!.repeatMode)
                dialog.cancel()
            }
            .show()

        reminderBinding.apply {
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

            getAndSetRepeatMode(reminderBinding)

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
                reminderAddTimeEdittext.setText(resources.getString(R.string.time_word))
                reminderAddDateEdittext.setText(resources.getString(R.string.date_word))
            }
        }

    }

    fun setTimeDeadline() {
        val deadlineBinding: AlertdialogAddDeadlineTimeBinding =
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
            .setView(deadlineBinding.root)
            .setCancelable(false)
            .setPositiveButton(resources.getString(R.string.save)) { dialog, _ ->
                if (taskDetailsViewModel.newDeadline.value != GLOBAL_START_DATE) {
                    if (taskDetailsViewModel.newDeadline.value != taskDetailsViewModel.bufferTask.value!!.deadline) {
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
                                    getString(
                                        R.string.deadline_for_day_hour_minute,
                                        resources.getQuantityString(
                                            R.plurals.days,
                                            days.toInt(),
                                            days
                                        ),
                                        resources.getQuantityString(
                                            R.plurals.hours,
                                            hours.toInt(),
                                            hours
                                        ),
                                        resources.getQuantityString(
                                            R.plurals.minutes,
                                            minutes.toInt(),
                                            minutes
                                        )
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            hours != 0L -> {
                                Toast.makeText(
                                    context,
                                    getString(
                                        R.string.deadline_for_hour_minute,
                                        resources.getQuantityString(
                                            R.plurals.hours,
                                            hours.toInt(),
                                            hours
                                        ),
                                        resources.getQuantityString(
                                            R.plurals.minutes,
                                            minutes.toInt(),
                                            minutes
                                        )
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            hours == 0L -> {
                                if (minutes == 0L) {
                                    Toast.makeText(
                                        context,
                                        getString(R.string.deadline_less_than_minute),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        getString(
                                            R.string.deadline_for_minutes,
                                            resources.getQuantityString(
                                                R.plurals.minutes,
                                                minutes.toInt(),
                                                minutes
                                            )
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            else -> {
                                Toast.makeText(
                                    context,
                                    getString(
                                        R.string.deadline_for_day_hour_minute,
                                        resources.getQuantityString(
                                            R.plurals.days,
                                            days.toInt(),
                                            days
                                        ),
                                        resources.getQuantityString(
                                            R.plurals.hours,
                                            hours.toInt(),
                                            hours
                                        ),
                                        resources.getQuantityString(
                                            R.plurals.minutes,
                                            minutes.toInt(),
                                            minutes
                                        )
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        binding.deadline.setText(
                            DateFormat.getDateTimeInstance(
                                DateFormat.DEFAULT,
                                DateFormat.DEFAULT,
                                resources.configuration.locales[0]
                            ).format(
                                Date(setTime.timeInMillis)
                            )
                        )
                    }
                } else {
                    binding.deadline.setText(
                        resources.getString(R.string.set_deadline)
                    )
                }
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel))
            { dialog, _ ->
                taskDetailsViewModel.updateMissedValue(taskDetailsViewModel.bufferTask.value!!.missed)
                dialog.cancel()
            }
            .show()

        deadlineBinding.apply {
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
                taskDetailsViewModel.updateMissedValue(false)
                binding.deadline.setText(resources.getString(R.string.set_deadline))
                addDeadlineDialog.cancel()
                Toast.makeText(
                    context,
                    resources.getString(R.string.deadline_task_removed),
                    Toast.LENGTH_SHORT
                )
                    .show()
                deadlineAddTimeEdittext.setText(resources.getString(R.string.time_word))
                deadlineAddDateEdittext.setText(resources.getString(R.string.date_word))
            }
        }
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
        val isSystem24Hour = is24HourFormat(context)
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(calendar[Calendar.HOUR_OF_DAY])
            .setMinute(calendar[Calendar.MINUTE])
            .setTitleText(resources.getString(R.string.select_time))
            .build()

        picker.addOnPositiveButtonClickListener {
            calendar[Calendar.HOUR_OF_DAY] = picker.hour
            calendar[Calendar.MINUTE] = picker.minute
            if (reminderBinding != null) {
                reminderBinding.reminderAddTimeEdittext.setText(
                    DateFormat.getTimeInstance(
                        DateFormat.DEFAULT,
                        resources.configuration.locales[0]
                    ).format(
                        Date(calendar.timeInMillis)
                    )
                )
                taskDetailsViewModel.updateTimeValue(calendar.timeInMillis)
            } else {
                deadlineTimeBinding!!.deadlineAddTimeEdittext.setText(
                    DateFormat.getTimeInstance(
                        DateFormat.DEFAULT,
                        resources.configuration.locales[0]
                    ).format(
                        Date(calendar.timeInMillis)
                    )
                )
                taskDetailsViewModel.updateDeadlineValue(calendar.timeInMillis)
                taskDetailsViewModel.updateMissedValue(false)
            }
        }
        picker.show(requireActivity().supportFragmentManager, "tag")
        return calendar
    }

    private fun openDatePicker(
        deadlineTimeBinding: AlertdialogAddDeadlineTimeBinding?,
        reminderBinding: AlertdialogAddReminderTimeBinding?,
        calendar: Calendar
    ): Calendar {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(resources.getString(R.string.select_date))
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.now()).build()
            )
            .setSelection(calendar.timeInMillis)
            .build()
        picker.addOnPositiveButtonClickListener {
            val dateCalendar = Calendar.getInstance().apply { timeInMillis = it }
            calendar.set(
                dateCalendar[Calendar.YEAR],
                dateCalendar[Calendar.MONTH],
                dateCalendar[Calendar.DAY_OF_MONTH]
            )
            if (reminderBinding != null) {
                reminderBinding.reminderAddDateEdittext.setText(
                    DateFormat.getDateInstance(
                        DateFormat.DEFAULT,
                        resources.configuration.locales[0]
                    ).format(
                        Date(calendar.timeInMillis)
                    )
                )
                taskDetailsViewModel.updateTimeValue(calendar.timeInMillis)
            } else {
                deadlineTimeBinding!!.deadlineAddDateEdittext.setText(
                    DateFormat.getDateInstance(
                        DateFormat.DEFAULT,
                        resources.configuration.locales[0]
                    ).format(
                        Date(calendar.timeInMillis)
                    )
                )
                taskDetailsViewModel.updateDeadlineValue(calendar.timeInMillis)
                taskDetailsViewModel.updateMissedValue(false)
            }
        }
        picker.show(requireActivity().supportFragmentManager, "tag")

        return calendar
    }

    private fun isEdited(): Boolean {
        binding.apply {
            taskDetailsViewModel.apply {
                bufferedTask?.let { task ->
                    return title.text.toString() != task.title ||
                            desc.text.toString() != task.description ||
                            checkTask.isChecked != task.checked ||
                            newTime.value != task.time ||
                            newRepeatMode.value != task.repeatMode ||
                            newDeadline.value != task.deadline ||
                            newPriority.value != task.priority ||
                            newCompleted.value != task.completed ||
                            newTotalChecked.value != task.totalChecked
                }
            }
        }
        return false
    }

    private fun updateTask() {
        binding.apply {
            taskDetailsViewModel.apply {
                if (isEdited()) {
                    bufferedTask?.let {bufferedTask ->
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
                        if (newTime.value != bufferedTask.time) {
                            if (newTime.value != GLOBAL_START_DATE) {
                                alarmMethods.setReminder(
                                    requireContext(),
                                    bufferedTask.idTask,
                                    newRepeatMode.value!!,
                                    newTime.value!!
                                )
                            } else {
                                alarmMethods.cancelReminder(requireContext(), bufferedTask.idTask)
                            }
                        }
                        if (newDeadline.value != bufferedTask.deadline) {
                            if (newDeadline.value != GLOBAL_START_DATE) {
                                alarmMethods.setDeadline(
                                    requireContext(), bufferedTask.idTask, newDeadline.value!!
                                )
                            } else {
                                alarmMethods.cancelDeadline(requireContext(), bufferedTask.idTask)
                            }
                        }

                        sharedViewModel.updateTask(
                            bufferedTask,
                            title.text.toString(),
                            desc.text.toString(),
                            newTime.value!!,
                            newRepeatMode.value!!,
                            newDeadline.value!!,
                            newPriority.value!!,
                            checkTask.isChecked,
                            newTotalChecked.value!!,
                            newCompleted.value!!,
                            newMissed.value!!,
                            newLastOvercheck.value!!
                        )
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.saved),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                bufferedTask!!,
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
        bufferedTask = null
    }
}