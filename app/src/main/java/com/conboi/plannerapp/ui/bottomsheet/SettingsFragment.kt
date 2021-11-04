package com.conboi.plannerapp.ui.bottomsheet

import android.app.NotificationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentSettingsBinding
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.cancelAllNotifications
import com.conboi.plannerapp.utils.updateLocale
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*

@AndroidEntryPoint
class SettingsFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by viewModels()

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
            deleteTasks.setOnClickListener { deleteAllTasks() }
            removeReminders.setOnClickListener { removeAllReminders() }
            getAndSetLanguage()
        }
    }

    @ExperimentalCoroutinesApi
    fun getAndSetLanguage() {
        binding.apply {
            var bufferLanguage = Locale.getDefault()
            dropLanguage.setOnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> {
                        if ("en" != sharedViewModel.appLanguage.value!!) {
                            bufferLanguage = Locale("en", "EN")
                            saveLanguage.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.primaryLightColorTree
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
                                    R.color.primaryLightColorTree
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
                                    R.color.primaryLightColorTree
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
        }
    }

    private fun deleteAllTasks() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.confirm_deletion))
            .setMessage(resources.getString(R.string.you_delete_tasks))
            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                sharedViewModel.deleteAllTasks()
                Toast.makeText(
                    context,
                    resources.getString(R.string.tasks_deleted),
                    Toast.LENGTH_SHORT
                ).show()
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
                val notificationManager = ContextCompat.getSystemService(
                    requireContext(),
                    NotificationManager::class.java
                )
                notificationManager!!.cancelAllNotifications(requireContext())
                dialog.dismiss()
                Toast.makeText(
                    context,
                    resources.getString(R.string.reminders_removed),
                    Toast.LENGTH_SHORT
                ).show()
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