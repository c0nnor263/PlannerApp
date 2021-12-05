package com.conboi.plannerapp.ui.bottomsheet

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentSettingsBinding
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.myclass.AlarmMethods
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class SettingsFragment : BottomSheetDialogFragment() {
    @Inject lateinit var alarmMethods:AlarmMethods
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            lifecycleOwner = this@SettingsFragment
            viewModel = sharedViewModel
            settingsFragment = this@SettingsFragment
            toolbar.setNavigationOnClickListener {
                dismiss()
            }
            privateMode.setOnClickListener {
                sharedViewModel.updatePrivateModeState(!sharedViewModel.privateModeState.value!!)
            }
            vibrations.setOnClickListener {
                sharedViewModel.updateVibrationModeState(!sharedViewModel.vibrationModeState.value!!)
            }
            reminders.setOnClickListener {
                sharedViewModel.updateRemindersModeState(!sharedViewModel.remindersModeState.value!!)
            }
            notifications.setOnClickListener {
                sharedViewModel.updateNotificationsModeState(!sharedViewModel.notificationsModeState.value!!)
            }

            val sharedPref =
                activity?.getSharedPreferences(ALARMS_FILE, Context.MODE_PRIVATE) ?: return
            if (sharedPref.getBoolean(ALARMS_FILE_INITIALIZED, false)) {
                removeReminders.isEnabled = true
                removeReminders.alpha = 1.0F
                removeReminders.setOnClickListener { removeAllReminders() }
            } else {
                removeReminders.isEnabled = false
                removeReminders.alpha = 0.5F
            }
            if (sharedViewModel.allTasksSize.value!! > 0) {
                deleteTasks.isEnabled = true
                deleteTasks.alpha = 1.0F
                deleteTasks.setOnClickListener { deleteAllTasks() }
            } else {
                deleteTasks.isEnabled = false
                deleteTasks.alpha = 0.5F
            }

            var bufferLanguage = Locale.getDefault()
            dropLanguage.setOnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> {
                        if ("en" != sharedViewModel.appLanguage.value!!) {
                            bufferLanguage = Locale("en", "EN")
                            saveLanguage.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.primaryDarkColorTree
                                )
                            )
                            saveLanguage.isEnabled = true
                        } else {
                            saveLanguage.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.primaryDarkColorAir
                                )
                            )
                            saveLanguage.isEnabled = false
                        }
                    }
                    1 -> {
                        if ("ru" != sharedViewModel.appLanguage.value!!) {
                            bufferLanguage = Locale("ru", "RU")
                            saveLanguage.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.primaryDarkColorTree
                                )
                            )
                            saveLanguage.isEnabled = true
                        } else {
                            saveLanguage.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.primaryDarkColorAir
                                )
                            )
                            saveLanguage.isEnabled = false
                        }
                    }
                    2 -> {
                        if ("zh" != sharedViewModel.appLanguage.value!!) {
                            bufferLanguage = Locale("zh", "CH")
                            saveLanguage.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.primaryDarkColorTree
                                )
                            )
                            saveLanguage.isEnabled = true
                        } else {
                            saveLanguage.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.primaryDarkColorAir
                                )
                            )
                            saveLanguage.isEnabled = false
                        }
                    }
                }
            }
            saveLanguage.setOnClickListener {
                sharedViewModel.updateLanguageState(bufferLanguage.language)
                updateLocale(requireContext(), bufferLanguage)
                (activity as MainActivity).invalidateOptionsMenu()
                dismiss()
                saveLanguage.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primaryDarkColorAir
                    )
                )
                saveLanguage.isEnabled = false
            }
            sharedViewModel.privateModeState.observe(this@SettingsFragment) { privateModeState ->
                privateMode.apply {
                    text = resources.getString(
                        R.string.private_state,
                        if (privateModeState) resources.getString(R.string.on) else resources.getString(
                            R.string.off
                        )
                    )
                    icon = if (privateModeState) {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryDarkColorTree
                            )
                        )
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_person_24)
                    } else {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryDarkColorAir
                            )
                        )
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_person_outline_24)
                    }
                }
            }
            sharedViewModel.vibrationModeState.observe(this@SettingsFragment) { vibrationState ->
                vibrations.apply {
                    text = resources.getString(
                        R.string.vibration_state,
                        if (vibrationState) resources.getString(R.string.on) else resources.getString(
                            R.string.off
                        )
                    )
                    icon = if (vibrationState) {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryDarkColorTree
                            )
                        )
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_vibration_24)
                    } else {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryDarkColorAir
                            )
                        )
                        ContextCompat.getDrawable(context, R.drawable.ic_vibrate_off_icon)
                    }
                }
            }
            sharedViewModel.remindersModeState.observe(this@SettingsFragment) { reminderState ->
                reminders.apply {
                    text = resources.getString(
                        R.string.reminders_state,
                        if (reminderState) resources.getString(R.string.on) else resources.getString(
                            R.string.off
                        )
                    )
                    icon = if (reminderState) {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryDarkColorTree
                            )
                        )
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_notifications_24)
                    } else {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryDarkColorAir
                            )
                        )
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_baseline_notifications_none_24
                        )
                    }
                }
            }
            sharedViewModel.notificationsModeState.observe(this@SettingsFragment) { notificationsState ->
                notifications.apply {
                    text = resources.getString(
                        R.string.notifications_state,
                        if (notificationsState) resources.getString(R.string.on) else resources.getString(
                            R.string.off
                        )
                    )
                    icon = if (notificationsState) {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryDarkColorTree
                            )
                        )
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_notifications_24)
                    } else {
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primaryDarkColorAir
                            )
                        )
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_baseline_notifications_off_24
                        )
                    }
                }

            }

        }
    }

    private fun deleteAllTasks() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.confirm_deletion))
            .setMessage(resources.getString(R.string.you_delete_tasks))
            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                sharedViewModel.deleteAllTasks(requireContext())
                Toast.makeText(
                    context,
                    resources.getString(R.string.tasks_deleted),
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()

    }

    private fun removeAllReminders() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.confirm_removing))
            .setMessage(resources.getString(R.string.you_remove_reminders))
            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                ContextCompat.getSystemService(
                    requireContext(),
                    NotificationManager::class.java
                ).apply {
                    this!!.cancelAll()
                }
                alarmMethods.cancelAllAlarmsType(requireContext(), remindersOrDeadlines = true)
                dialog.dismiss()
                Toast.makeText(
                    context,
                    resources.getString(R.string.reminders_removed),
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}