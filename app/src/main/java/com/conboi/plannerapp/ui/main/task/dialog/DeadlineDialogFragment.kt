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
import com.conboi.plannerapp.databinding.FragmentDeadlineDialogBinding
import com.conboi.plannerapp.interfaces.dialog.DeadlineDialogCallback
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.DateFormat
import java.util.*

class DeadlineDialogFragment(
    private val initialTask: TaskType,
    private val inCalendarTime: Long,
    val callback: DeadlineDialogCallback,
) : DialogFragment() {
    private var _binding: FragmentDeadlineDialogBinding? = null
    val binding get() = _binding!!

    val viewModel: DeadlineDialogViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentDeadlineDialogBinding.inflate(layoutInflater)

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
                    val newCalendar = viewModel.bufferCalendar.value!!
                    val newMissed = viewModel.bufferMissed.value!!

                    callback.saveDeadline(newCalendar, newMissed)
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

        val calendar = Calendar.getInstance().apply {
            timeInMillis =
                if (initialTask.deadline != GLOBAL_START_DATE) inCalendarTime else GLOBAL_START_DATE

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
        viewModel.updateBufferMissed(initialTask.missed)
        viewModel.updateBufferDeadline(initialTask.deadline)


        binding.ivBtnOpenTimepicker.setOnClickListener {
            viewModel.updateBufferCalendar(createTimePickerDeadline())
        }
        binding.ivBtnOpenDatepicker.setOnClickListener {
            viewModel.updateBufferCalendar(createDatePickerDeadline())
        }

        binding.mBtnRemoveDeadline.setOnClickListener {
            dismiss()

            callback.removeDeadline()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createTimePickerDeadline(): Calendar {
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
            binding.tietTimeDeadline.setText(
                DateFormat.getTimeInstance(
                    DateFormat.DEFAULT,
                    resources.configuration.locales[0]
                ).format(
                    Date(calendar.timeInMillis)
                )
            )
            viewModel.updateBufferDeadline(calendar.timeInMillis)
            viewModel.updateBufferMissed(false)
        }
        picker.show(requireActivity().supportFragmentManager, "tag")
        return calendar
    }

    private fun createDatePickerDeadline(): Calendar {
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

            binding.tietDateDeadline.setText(
                DateFormat.getDateInstance(
                    DateFormat.DEFAULT,
                    resources.configuration.locales[0]
                ).format(
                    Date(calendar.timeInMillis)
                )
            )
            viewModel.updateBufferDeadline(calendar.timeInMillis)
            viewModel.updateBufferMissed(false)
        }
        picker.show(requireActivity().supportFragmentManager, "tag")
        return calendar
    }

    companion object {
        const val TAG = "DeadlineDialogFragment"
    }
}