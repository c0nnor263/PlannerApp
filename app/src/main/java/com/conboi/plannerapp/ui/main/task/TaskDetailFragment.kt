package com.conboi.plannerapp.ui.main.task

import android.app.AlarmManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentTaskDetailBinding
import com.conboi.plannerapp.interfaces.UpdateTotalTaskCallback
import com.conboi.plannerapp.interfaces.dialog.DeadlineDialogCallback
import com.conboi.plannerapp.interfaces.dialog.ReminderDialogCallback
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class TaskDetailFragment : Fragment(), ReminderDialogCallback, DeadlineDialogCallback {
    private var _binding: FragmentTaskDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskDetailViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.navigation_host
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().themeColor(com.google.android.material.R.attr.colorSurface))
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            viewModel.sendCancelExitEvent()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_task_detail, container, false)
        binding.toolbar.setNavigationOnClickListener {
            viewModel.sendCancelExitEvent()
        }

        // CheckBox
        binding.checkBox.apply {
            setOnClickListener {
                checkBoxClick()
            }
            setOnLongClickListener {
                checkBoxHold()
            }
        }

        // Title and description
        binding.tietTitle.addTextChangedListener {
            viewModel.updateTitleValue(it.toString())
        }
        binding.tietDesc.addTextChangedListener {
            viewModel.updateDescriptionValue(it.toString())
        }

        // Priority
        binding.actvPriority.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> updatePriority(R.color.primaryDarkColorAir, Priority.LEISURELY)
                1 -> updatePriority(R.color.secondaryColorWater, Priority.DEFAULT)
                2 -> updatePriority(R.color.primaryLightColorFire, Priority.ADVISABLE)
                3 -> updatePriority(R.color.secondaryDarkColorFire, Priority.IMPORTANT)
                else -> updatePriority(R.color.secondaryColorWater, Priority.DEFAULT)
            }
        }

        // Reminder and deadline
        binding.tietTime.setOnClickListener { viewModel.sendSetTimeReminderEvent() }
        binding.tietDeadline.setOnClickListener { viewModel.sendSetTimeDeadlineEvent() }

        (activity as MainActivity).binding.fabMain.apply {
            setOnClickListener {
                updateTask()
            }
            setOnLongClickListener(null)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.layoutViewModel = viewModel


        val navigationArgs: TaskDetailFragmentArgs by navArgs()
        val id = navigationArgs.idTask

        viewModel.getTask(id).observe(viewLifecycleOwner) { task ->
                binding.checkBox.isChecked = task.checked
                viewModel.setInitialTask(task)
        }

        viewModel.newChecked.observe(viewLifecycleOwner) {
            checkedStateUI(it)
        }

        viewModel.newMissed.observe(viewLifecycleOwner) {
            missedStateUI(it)
        }

        viewModel.reminderState.observe(viewLifecycleOwner) {
            if (!it) {
                binding.tilTime.isEnabled = false
                binding.tietTime.setText(resources.getString(R.string.reminders_are_off))
                binding.tietTime.setOnClickListener(null)

                binding.tilDeadline.isEnabled = false
                binding.tietDeadline.setText(resources.getString(R.string.reminders_are_off))
                binding.tietDeadline.setOnClickListener(null)
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        TaskDetailViewModel.TaskDetailEvent.ShowCancelExit -> {
                            cancelEdit()
                            viewModel.sendCancelExitEvent(null)
                        }
                        TaskDetailViewModel.TaskDetailEvent.ShowSetTimeReminder -> {
                            setTimeReminder()
                            viewModel.sendSetTimeReminderEvent(null)
                        }
                        TaskDetailViewModel.TaskDetailEvent.ShowSetTimeDeadline -> {
                            setTimeDeadline()
                            viewModel.sendSetTimeDeadlineEvent(null)
                        }
                        null -> {}
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveState()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewModel.retrieveState()
        binding.checkBox.isChecked = viewModel.newChecked.value == true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun saveReminder(calendar: Calendar) {
        val initialTime = viewModel.initialTask.value!!.time
        val newTime = viewModel.newTime.value!!
        if (newTime != initialTime) {
            calculateRemainingTime(calendar, AlarmType.REMINDER)
            binding.tietTime.setText(
                DateFormat.getDateTimeInstance(
                    DateFormat.DEFAULT,
                    DateFormat.DEFAULT,
                    resources.configuration.locales[0]
                )
                    .format(
                        Date(newTime)
                    )
            )
        }
    }

    override fun removeReminder() {
        Toast.makeText(
            requireContext(),
            resources.getString(R.string.reminder_task_removed),
            Toast.LENGTH_SHORT
        ).show()
        binding.tietTime.setText(resources.getString(R.string.set_reminder))
    }

    override fun saveDeadline(calendar: Calendar) {
        val initialDeadline = viewModel.initialTask.value!!.deadline
        val newDeadline = viewModel.newDeadline.value!!

        if (newDeadline != initialDeadline) {
            calculateRemainingTime(calendar, AlarmType.DEADLINE)
            binding.tietTime.setText(
                DateFormat.getDateTimeInstance(
                    DateFormat.DEFAULT,
                    DateFormat.DEFAULT,
                    resources.configuration.locales[0]
                )
                    .format(
                        Date(newDeadline)
                    )
            )
        }
    }

    override fun removeDeadline() {
        Toast.makeText(
            requireContext(),
            resources.getString(R.string.deadline_task_removed),
            Toast.LENGTH_SHORT
        ).show()
        binding.tietDeadline.setText(resources.getString(R.string.set_deadline))
    }


    private fun updatePriority(@ColorRes colorId: Int, position: Priority) {
        viewModel.updatePriorityValue(position)
        binding.actvPriority.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                colorId
            )
        )
    }

    private fun checkBoxClick() {
        if (viewModel.newTitle.value?.isBlank() == true) {
            showCantCheckDialog(requireContext())
            binding.checkBox.isChecked = false
            viewModel.updateCheckedValue(false)
        }
    }

    private fun checkBoxHold() = with(binding) {
        val lastOvercheck = viewModel.newLastOvercheck.value!!

        if (System.currentTimeMillis() - lastOvercheck >= AlarmManager.INTERVAL_DAY) {
            (activity as MainActivity).vibrateDefaultAmplitude(2, HOLD_VIBRATION)

            if (viewModel.newTitle.value?.isNotBlank() == true) {
                viewModel.updateCheckedValue(true)
            } else {
                showCantCheckDialog(requireContext())
                binding.checkBox.isChecked = false
                viewModel.updateCheckedValue(false)
            }
        } else {
            showCantOvercheckDialog(requireContext())
        }
        true
    }


    private fun setTimeReminder() {
        val adReminderFragment = ReminderDialogFragment(this)
        adReminderFragment.show(parentFragmentManager, ReminderDialogFragment.TAG)
    }


    private fun setTimeDeadline() {
        val adDeadlineFragment = DeadlineDialogFragment(this)
        adDeadlineFragment.show(parentFragmentManager, DeadlineDialogFragment.TAG)
    }


    private fun updateTask() {
        if (viewModel.isEdited()) {
            viewModel.updateTask(requireContext(), object : UpdateTotalTaskCallback {
                override fun onIncrement(differ: Int) {
                    viewModel.incrementTotalCompleted(differ)
                }

                override fun onDecrement(differ: Int) {
                    viewModel.decrementTotalCompleted(differ)
                }
            })

            Toast.makeText(
                requireContext(),
                resources.getString(R.string.saved),
                Toast.LENGTH_SHORT
            ).show()

        }
        findNavController().navigateUp()
    }

    private fun cancelEdit() {
        if (viewModel.isEdited()) {
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


    private fun calculateRemainingTime(calendar: Calendar, alarmType: AlarmType) {
        var remainingTime: Long
        var minute = 0L
        var hour = 0L
        var day = 0L

        fun updateRemainingTime() {
            remainingTime =
                calendar.timeInMillis - Calendar.getInstance().timeInMillis
            minute = (remainingTime / (1000 * 60) % 60)
            hour = (remainingTime / (1000 * 60 * 60) % 24)
            day = TimeUnit.MILLISECONDS.toDays(remainingTime)
        }

        updateRemainingTime()

        if (minute < 0L || hour < 0L || day < 0L
        ) {
            calendar.add(Calendar.DATE, 1)
            updateRemainingTime()
            when (alarmType) {
                AlarmType.REMINDER -> {
                    viewModel.updateTimeValue(calendar.timeInMillis)
                    showRemainingTimeReminderToast(day, hour, minute)
                }
                AlarmType.DEADLINE -> {
                    viewModel.updateDeadlineValue(calendar.timeInMillis)
                    showRemainingTimeDeadlineToast(day, hour, minute)
                }
                else -> {}
            }
        }

    }

    private fun showRemainingTimeReminderToast(day: Long, hour: Long, minute: Long) {
        val remainingString = when {
            day != 0.toLong() -> {
                getString(
                    R.string.reminder_for_day_hour_minute,
                    resources.getQuantityString(
                        R.plurals.days,
                        day.toInt(),
                        day
                    ),
                    resources.getQuantityString(
                        R.plurals.hours,
                        hour.toInt(),
                        hour
                    ),
                    resources.getQuantityString(
                        R.plurals.minutes,
                        minute.toInt(),
                        minute
                    )
                )
            }
            hour != 0.toLong() -> {
                getString(
                    R.string.reminder_for_hour_minute,
                    resources.getQuantityString(
                        R.plurals.hours,
                        hour.toInt(),
                        hour
                    ),
                    resources.getQuantityString(
                        R.plurals.minutes,
                        minute.toInt(),
                        minute
                    )
                )
            }
            hour == 0.toLong() -> {
                if (minute == 0.toLong()) {
                    getString(R.string.reminder_less_than_minute)
                } else {
                    getString(
                        R.string.reminder_for_minutes,
                        resources.getQuantityString(
                            R.plurals.minutes,
                            minute.toInt(),
                            minute
                        )
                    )
                }
            }
            else -> {
                getString(
                    R.string.reminder_for_day_hour_minute,
                    resources.getQuantityString(
                        R.plurals.days,
                        day.toInt(),
                        day
                    ),
                    resources.getQuantityString(
                        R.plurals.hours,
                        hour.toInt(),
                        hour
                    ),
                    resources.getQuantityString(
                        R.plurals.minutes,
                        minute.toInt(),
                        minute
                    )
                )
            }
        }
        Toast.makeText(
            requireContext(),
            remainingString,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showRemainingTimeDeadlineToast(day: Long, hour: Long, minute: Long) {
        val deadlineString = when {
            day != 0.toLong() -> {
                getString(
                    R.string.deadline_for_day_hour_minute,
                    resources.getQuantityString(
                        R.plurals.days,
                        day.toInt(),
                        day
                    ),
                    resources.getQuantityString(
                        R.plurals.hours,
                        hour.toInt(),
                        hour
                    ),
                    resources.getQuantityString(
                        R.plurals.minutes,
                        minute.toInt(),
                        minute
                    )
                )
            }
            hour != 0.toLong() -> {
                getString(
                    R.string.deadline_for_hour_minute,
                    resources.getQuantityString(
                        R.plurals.hours,
                        hour.toInt(),
                        hour
                    ),
                    resources.getQuantityString(
                        R.plurals.minutes,
                        minute.toInt(),
                        minute
                    )
                )
            }
            hour == 0.toLong() -> {
                if (minute == 0.toLong()) {
                    getString(R.string.deadline_less_than_minute)
                } else {
                    getString(
                        R.string.deadline_for_minutes,
                        resources.getQuantityString(
                            R.plurals.minutes,
                            minute.toInt(),
                            minute
                        )
                    )
                }
            }
            else -> {
                getString(
                    R.string.deadline_for_day_hour_minute,
                    resources.getQuantityString(
                        R.plurals.days,
                        day.toInt(),
                        day
                    ),
                    resources.getQuantityString(
                        R.plurals.hours,
                        hour.toInt(),
                        hour
                    ),
                    resources.getQuantityString(
                        R.plurals.minutes,
                        minute.toInt(),
                        minute
                    )
                )
            }
        }
        Toast.makeText(
            requireContext(),
            deadlineString,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun missedStateUI(state: Boolean) {
        if (state) {
            binding.tietDeadline.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.secondaryDarkColorFire
                )
            )
        } else {
            binding.tietDeadline.setTextColor(
                binding.tietTitle.textColors
            )
        }
    }

    private fun checkedStateUI(state: Boolean) = with(binding) {
        if (state) {
            tvCompleted.visibility = View.VISIBLE
        } else {
            tilTitle.isEnabled = true
            tilDesc.isEnabled = true
            tilTime.isEnabled = true
            tilDeadline.isEnabled = true
            tilPriority.isEnabled = true
        }
        checkBox.isChecked = state
    }
}