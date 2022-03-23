package com.conboi.plannerapp.ui.bottomsheet

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentBottomSettingsBinding
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.utils.AlarmType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class BottomSettingsFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentBottomSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BottomSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomSettingsBinding.inflate(layoutInflater)
        binding.apply {
            toolbar.setNavigationOnClickListener {
                dismiss()
            }

            binding.mBtnSaveLanguage.setOnClickListener {
                saveNewLanguage()
            }

            mBtnRemoveReminders.setOnClickListener {
                removeAllRemindersDialog()
            }
            mBtnDeleteTasks.setOnClickListener {
                deleteAllTasksDialog()
            }

            mBtnPrivate.setOnClickListener {
                viewModel.updatePrivateModeState()
            }
            mBtnVibration.setOnClickListener {
                viewModel.updateVibrationModeState()
            }
            mBtnReminder.setOnClickListener {
                viewModel.updateReminderModeState()
            }
            mBtnNotification.setOnClickListener {
                viewModel.updateNotificationModeState()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setActvChoiceLanguage()

        removeReminderUI()
        deleteAllTasksUI()

        viewModel.privateState.observe(this) { privateModeState ->
            buttonUpdateUI(
                button = binding.mBtnPrivate,
                state = privateModeState,
                textId = R.string.private_state,
                iconIdEnabled = R.drawable.ic_baseline_person_24,
                iconIdDisabled = R.drawable.ic_baseline_person_outline_24
            )
        }

        viewModel.vibrationState.observe(this) { vibrationState ->
            buttonUpdateUI(
                button = binding.mBtnVibration,
                state = vibrationState,
                textId = R.string.vibration_state,
                iconIdEnabled = R.drawable.ic_baseline_vibration_24,
                iconIdDisabled = R.drawable.ic_vibrate_off_icon
            )
        }

        viewModel.reminderState.observe(this) { reminderState ->
            buttonUpdateUI(
                button = binding.mBtnReminder,
                state = reminderState,
                textId = R.string.reminder_state,
                iconIdEnabled = R.drawable.ic_baseline_access_alarm_24,
                iconIdDisabled = R.drawable.ic_baseline_alarm_off_24
            )
        }

        viewModel.notificationState.observe(this) { notificationsState ->
            buttonUpdateUI(
                button = binding.mBtnNotification,
                state = notificationsState,
                textId = R.string.notification_state,
                iconIdEnabled = R.drawable.ic_baseline_notifications_24,
                iconIdDisabled = R.drawable.ic_baseline_notifications_off_24
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveState()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewModel.retrieveState()
        setActvChoiceLanguage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun setActvChoiceLanguage() = with(binding.actvLanguage) {
        val items = resources.getStringArray(R.array.languages)
        val selectedLanguage =
            viewModel.selectedLanguage.value?.language!!
        when (selectedLanguage) {
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
        if (viewModel.isLanguageCanChange(selectedLanguage)) {
            saveLanguageEnable()
        }


        val adapter = ArrayAdapter(context, R.layout.dropmenu_language, items)
        setAdapter(adapter)

        setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {
                    if (viewModel.isLanguageCanChange("en")) {
                        viewModel.updateSelectedLanguage(
                            Locale(
                                "en",
                                "EN"
                            )
                        )
                        saveLanguageEnable()
                    } else {
                        saveLanguageDisable()
                    }
                }
                1 -> {
                    if (viewModel.isLanguageCanChange("ru")) {
                        viewModel.updateSelectedLanguage(
                            Locale(
                                "ru",
                                "RU"
                            )
                        )
                        saveLanguageEnable()
                    } else {
                        saveLanguageDisable()
                    }
                }
                2 -> {
                    if (viewModel.isLanguageCanChange("uk")) {
                        viewModel.updateSelectedLanguage(
                            Locale(
                                "uk",
                                "UA"
                            )
                        )
                        saveLanguageEnable()
                    } else {
                        saveLanguageDisable()
                    }
                }
            }
        }
    }

    private fun buttonUpdateUI(
        button: MaterialButton,
        state: Boolean,
        @StringRes textId: Int,
        @DrawableRes iconIdEnabled: Int,
        @DrawableRes iconIdDisabled: Int
    ) {
        button.apply {
            text = resources.getString(
                textId,
                if (state) {
                    resources.getString(R.string.on)
                } else {
                    resources.getString(R.string.off)
                }
            )
            icon = if (state) {
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primaryDarkColorTree
                    )
                )
                ContextCompat.getDrawable(context, iconIdEnabled)
            } else {
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primaryDarkColorAir
                    )
                )
                ContextCompat.getDrawable(context, iconIdDisabled)
            }
        }
    }

    private fun deleteAllTasksUI() {
        viewModel.getTasksSize().observe(viewLifecycleOwner) {
            val isNotEmpty = it > 0
            if (isNotEmpty) {
                binding.mBtnDeleteTasks.isEnabled = true
                binding.mBtnDeleteTasks.alpha = 1.0F
            } else {
                binding.mBtnDeleteTasks.isEnabled = false
                binding.mBtnDeleteTasks.alpha = 0.5F
            }
        }

    }

    private fun removeReminderUI() = viewLifecycleOwner.lifecycleScope.launch {
        if (viewModel.isReminderAvailable()) {
            binding.mBtnRemoveReminders.isEnabled = true
            binding.mBtnRemoveReminders.alpha = 1.0F
        } else {
            binding.mBtnRemoveReminders.isEnabled = false
            binding.mBtnRemoveReminders.alpha = 0.5F
        }
    }


    private fun saveLanguageEnable() {
        binding.mBtnSaveLanguage.isEnabled = true
        binding.mBtnSaveLanguage.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.primaryDarkColorTree
            )
        )
    }

    private fun saveLanguageDisable() {
        binding.mBtnSaveLanguage.isEnabled = false
        binding.mBtnSaveLanguage.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.primaryDarkColorAir
            )
        )
    }


    @SuppressLint("SwitchIntDef")
    private fun saveNewLanguage() {
        saveLanguageDisable()
        val listener = SplitInstallStateUpdatedListener { state ->
            when (state.status()) {
                SplitInstallSessionStatus.INSTALLED -> {
                    viewModel.updateAppLanguage()
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

        val splitInstallManager = SplitInstallManagerFactory.create(requireContext())
        splitInstallManager.registerListener(listener)

        val request = SplitInstallRequest.newBuilder()
            .addLanguage(Locale.forLanguageTag(viewModel.selectedLanguage.value!!.language))
            .build()

        splitInstallManager.startInstall(request).addOnCompleteListener {
            splitInstallManager.unregisterListener(listener)
        }
    }

    private fun deleteAllTasksDialog(): AlertDialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.confirm_deletion))
            .setMessage(resources.getString(R.string.you_delete_tasks))
            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                viewModel.deleteAllTasks(requireContext())
                Toast.makeText(
                    context,
                    resources.getString(R.string.tasks_deleted),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
                dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()

    private fun removeAllRemindersDialog(): AlertDialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.confirm_removing))
            .setMessage(resources.getString(R.string.you_remove_reminders))
            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                ContextCompat.getSystemService(
                    requireContext(),
                    NotificationManager::class.java
                )?.cancelAll()

                viewModel.cancelAllAlarmsType(
                    requireContext(),
                    AlarmType.REMINDER
                )

                Toast.makeText(
                    context,
                    resources.getString(R.string.reminders_removed),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
                dismiss()
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
}