package com.conboi.plannerapp.ui.main.task.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.databinding.FragmentReminderDialogBinding
import com.conboi.plannerapp.interfaces.dialog.ReminderDialogCallback
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.RepeatMode
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class ReminderDialogFragment(
    private val initialTask: TaskType,
    private val inCalendarTime: Long,
    val callback: ReminderDialogCallback,
) : DialogFragment() {
    private var _binding: FragmentReminderDialogBinding? = null
    val binding get() = _binding!!

    val viewModel: ReminderDialogViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentReminderDialogBinding.inflate(layoutInflater)
        val thisDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.add_reminder))
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton(resources.getString(R.string.save), null)
            .setNegativeButton(resources.getString(R.string.cancel), null)
            .create()

        thisDialog.setOnShowListener { dialog ->
            val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveBtn.setOnClickListener {
                val newCalendar = viewModel.bufferCalendar.value!!
                val newRepeatMode = viewModel.bufferRepeatMode.value!!

                callback.saveReminder(newCalendar, newRepeatMode)
                dismiss()
            }

            negativeBtn.setOnClickListener {
                dismiss()
            }
        }
        return thisDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.layoutViewModel = viewModel

        val calendar = Calendar.getInstance().apply {
            timeInMillis =
                if (initialTask.time != GLOBAL_START_DATE) inCalendarTime else GLOBAL_START_DATE
            set(
                Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR)
            )
            set(
                Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH)
            )
            set(
                Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            )
        }

        viewModel.updateBufferCalendar(calendar)
        viewModel.updateBufferTime(initialTask.time)
        viewModel.updateBufferRepeatMode(initialTask.repeatMode)

        // TaskDetail
        binding.ivBtnOpenTimepicker.setOnClickListener {
            viewModel.updateBufferCalendar(createTimePickerReminder())
        }
        binding.ivBtnOpenDatepicker.setOnClickListener {
            viewModel.updateBufferCalendar(createDatePickerReminder())
        }

        binding.actvRepeatReminder.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateBufferRepeatMode(
                when (position) {
                    0 -> RepeatMode.Once
                    1 -> RepeatMode.Daily
                    2 -> RepeatMode.Weekly
                    else -> RepeatMode.Once
                }
            )
        }

        binding.mBtnRemoveReminder.setOnClickListener {
            dismiss()

            callback.removeReminder()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createTimePickerReminder(): Calendar {
        val calendar = viewModel.bufferCalendar.value!!
        val clockFormat =
            if (is24HourFormat(context)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val picker = MaterialTimePicker.Builder()
            .setTitleText(resources.getString(R.string.select_time))
            .setTimeFormat(clockFormat)
            .setHour(calendar[Calendar.HOUR_OF_DAY])
            .setMinute(calendar[Calendar.MINUTE])
            .build()

        picker.addOnPositiveButtonClickListener {
            calendar[Calendar.HOUR_OF_DAY] = picker.hour
            calendar[Calendar.MINUTE] = picker.minute

            viewModel.updateBufferTime(calendar.timeInMillis)
        }
        picker.show(requireActivity().supportFragmentManager, "tag")
        return calendar
    }

    private fun createDatePickerReminder(): Calendar {
        val calendar = viewModel.bufferCalendar.value!!
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

            viewModel.updateBufferTime(calendar.timeInMillis)
        }

        picker.show(requireActivity().supportFragmentManager, "tag")
        return calendar
    }

    companion object {
        const val TAG = "ReminderDialogFragment"
    }
}