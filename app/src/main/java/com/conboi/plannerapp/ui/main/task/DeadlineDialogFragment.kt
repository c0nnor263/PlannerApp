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
import com.conboi.plannerapp.databinding.FragmentDeadlineDialogBinding
import com.conboi.plannerapp.interfaces.dialog.DeadlineDialogCallback
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.*

@AndroidEntryPoint
class DeadlineDialogFragment(
    val callback: DeadlineDialogCallback
) : DialogFragment() {
    private var _binding: FragmentDeadlineDialogBinding? = null
    val binding get() = _binding!!

    val viewModel: TaskDetailViewModel by activityViewModels()

    private lateinit var newCalendar: Calendar

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentDeadlineDialogBinding.inflate(layoutInflater)
        val initialDeadline = viewModel.initialTask.value!!.deadline

        newCalendar = Calendar.getInstance().apply {
            timeInMillis =
                if (initialDeadline != GLOBAL_START_DATE) initialDeadline else GLOBAL_START_DATE

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
            .setTitle(resources.getString(R.string.add_deadline))
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton(resources.getString(R.string.save), null)
            .setNegativeButton(resources.getString(R.string.cancel), null)
            .create()
        thisDialog
            .setOnShowListener { dialog ->
                val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                positiveBtn.setOnClickListener {
                    callback.saveDeadline(newCalendar)
                    dialog.dismiss()
                }
                negativeBtn.setOnClickListener {
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

        binding.ivBtnOpenTimepicker.setOnClickListener {
            newCalendar = createTimePickerDeadline(newCalendar)
        }
        binding.ivBtnOpenDatepicker.setOnClickListener {
            newCalendar = createDatePickerDeadline(newCalendar)
        }

        binding.mBtnRemoveDeadline.setOnClickListener {
            viewModel.removeDeadline()
            dismiss()

            binding.tietTimeDeadline.setText(resources.getString(R.string.time_word))
            binding.tietDateDeadline.setText(resources.getString(R.string.date_word))

            callback.removeDeadline()
        }
    }


    private fun createTimePickerDeadline(
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
            binding.tietTimeDeadline.setText(
                DateFormat.getTimeInstance(
                    DateFormat.DEFAULT,
                    resources.configuration.locales[0]
                ).format(
                    Date(calendar.timeInMillis)
                )
            )
            viewModel.updateDeadlineValue(calendar.timeInMillis)
            viewModel.updateMissedValue(false)
        }
        picker.show(requireActivity().supportFragmentManager, "tag")
        return calendar
    }

    private fun createDatePickerDeadline(
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

            binding.tietDateDeadline.setText(
                DateFormat.getDateInstance(
                    DateFormat.DEFAULT,
                    resources.configuration.locales[0]
                ).format(
                    Date(calendar.timeInMillis)
                )
            )
            viewModel.updateDeadlineValue(calendar.timeInMillis)
            viewModel.updateMissedValue(false)
        }
        picker.show(requireActivity().supportFragmentManager, "tag")
        return calendar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DeadlineDialogFragment"
    }
}