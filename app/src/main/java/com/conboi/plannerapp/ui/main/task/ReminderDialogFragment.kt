package com.conboi.plannerapp.ui.main.task

import android.app.Dialog
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.conboi.plannerapp.R
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
import java.text.DateFormat
import java.util.*


class ReminderDialogFragment(
    val callback: ReminderDialogCallback
) : DialogFragment() {
    private var _binding: FragmentReminderDialogBinding? = null
    val binding get() = _binding!!

    val viewModel: TaskDetailViewModel by activityViewModels()

    private lateinit var newCalendar: Calendar

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentReminderDialogBinding.inflate(layoutInflater)

        val initialTime = viewModel.initialTask.value!!.time
        val initialRepeatMode = viewModel.initialTask.value!!.repeatMode
        newCalendar = Calendar.getInstance().apply {
            timeInMillis = if (initialTime != GLOBAL_START_DATE) initialTime else GLOBAL_START_DATE

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
                callback.saveReminder(newCalendar)
                dialog.dismiss()
            }

            negativeBtn.setOnClickListener {
                viewModel.updateRepeatModeValue(initialRepeatMode)
                dialog.cancel()
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

        // TaskDetail
        binding.ivBtnOpenTimepicker.setOnClickListener {
            newCalendar = createTimePickerReminder(newCalendar)
        }
        binding.ivBtnOpenDatepicker.setOnClickListener {
            newCalendar = createDatePickerReminder(newCalendar)
        }

        binding.actvRepeatReminder.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateRepeatModeValue(
                when (position) {
                    0 -> RepeatMode.Once
                    1 -> RepeatMode.Daily
                    2 -> RepeatMode.Weekly
                    else -> RepeatMode.Once
                }
            )
        }

        binding.mBtnRemoveReminder.setOnClickListener {
            viewModel.removeReminder()
            dismiss()
            binding.tietTimeReminder.setText(resources.getString(R.string.time_word))
            binding.tietDateReminder.setText(resources.getString(R.string.date_word))


            callback.removeReminder()
        }
    }

    private fun createTimePickerReminder(
        calendar: Calendar
    ): Calendar {
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
            binding.tietTimeReminder.setText(
                DateFormat.getTimeInstance(
                    DateFormat.DEFAULT,
                    resources.configuration.locales[0]
                ).format(
                    Date(calendar.timeInMillis)
                )
            )
            viewModel.updateTimeValue(calendar.timeInMillis)
        }
        picker.show(requireActivity().supportFragmentManager, "tag")
        return calendar
    }

    private fun createDatePickerReminder(
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

            binding.tietDateReminder.setText(
                DateFormat.getDateInstance(
                    DateFormat.DEFAULT,
                    resources.configuration.locales[0]
                ).format(
                    Date(calendar.timeInMillis)
                )
            )
            viewModel.updateTimeValue(calendar.timeInMillis)
        }

        picker.show(requireActivity().supportFragmentManager, "tag")
        return calendar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ReminderDialogFragment"
    }
}