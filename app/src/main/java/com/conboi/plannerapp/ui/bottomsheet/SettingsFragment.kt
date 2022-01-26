package com.conboi.plannerapp.ui.bottomsheet

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentSettingsBinding
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.myclass.AlarmMethods
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class SettingsFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var alarmMethods: AlarmMethods
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
        binding.apply {
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
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            lifecycleOwner = this@SettingsFragment
            viewModel = sharedViewModel
            settingsFragment = this@SettingsFragment

            val sharedPref =
                activity?.getSharedPreferences(ALARM_FILE, Context.MODE_PRIVATE) ?: return
            val activityPref =
                PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)

            val alarmFileInitialized = sharedPref.getBoolean(ALARM_FILE_INITIALIZED, false)
            val setLanguage = activityPref.getString(LANGUAGE, Locale.getDefault().language)
            var bufferLanguage = Locale.getDefault()

            dropLanguage.apply {
                setOnItemClickListener { _, _, position, _ ->
                    when (position) {
                        0 -> {
                            if ("en" != setLanguage) {
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
                            if ("ru" != setLanguage) {
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
                            if ("uk" != setLanguage) {
                                bufferLanguage = Locale("uk", "UA")
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
                val items = resources.getStringArray(R.array.languages)
                when (setLanguage) {
                    "en" -> {
                        setText(items[0])
                    }
                    "ru" -> {
                        setText(items[1])
                    }
                    "uk" -> {
                        setText(items[2])
                    }
                }
                val adapter = ArrayAdapter(context, R.layout.dropmenu_language, items)
                setAdapter(adapter)
            }


            if (alarmFileInitialized && sharedPref.all.isNotEmpty()) {
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


            saveLanguage.setOnClickListener {

                saveLanguage.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primaryDarkColorAir
                    )
                )
                saveLanguage.isEnabled = false

                val splitInstallManager = SplitInstallManagerFactory.create(requireContext())
                val listener = SplitInstallStateUpdatedListener { state ->
                    when (state.status()) {
                        SplitInstallSessionStatus.INSTALLED -> {
                            activityPref.edit().putString(LANGUAGE, bufferLanguage.language)
                                .apply()
                            dismiss()
                            (requireActivity() as MainActivity).recreate()
                        }
                        SplitInstallSessionStatus.FAILED -> {
                            Toast.makeText(
                                requireContext(),
                                state.errorCode().toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                val request = SplitInstallRequest.newBuilder()
                    .addLanguage(Locale.forLanguageTag(bufferLanguage.language))
                    .build()
                splitInstallManager.registerListener(listener)
                splitInstallManager.startInstall(request).addOnCompleteListener {
                    splitInstallManager.unregisterListener(listener)
                }
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