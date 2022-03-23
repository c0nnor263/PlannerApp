package com.conboi.plannerapp.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentEditProfileDialogBinding
import com.conboi.plannerapp.interfaces.dialog.EditProfileDialogCallback
import com.conboi.plannerapp.utils.showErrorToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EditProfileDialogFragment(val callback: EditProfileDialogCallback) : DialogFragment() {
    private var _binding: FragmentEditProfileDialogBinding? = null
    val binding get() = _binding!!

    val viewModel: ProfileViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentEditProfileDialogBinding.inflate(layoutInflater)

        val thisDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle(resources.getString(R.string.edit_profile))
            .setCancelable(false)
            .setPositiveButton(resources.getString(R.string.save), null)
            .setNegativeButton(resources.getString(R.string.cancel), null)
            .create()

        thisDialog.setOnShowListener { dialog ->
            val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setOnClickListener {
                val newName = binding.tietNewName.text.toString()
                val newEmail = binding.tietNewEmail.text.toString().trim()
                val newPassword = binding.tietNewPassword.text.toString()
                val currentPassword = binding.tietCurrentPassword.text.toString()
                val user = viewModel.user ?: return@setOnClickListener

                if (newName.isNotBlank() || newEmail.isNotBlank() || newPassword.isNotBlank()) {

                    if (newName.isNotBlank()) {
                        if (newName != user.displayName.toString()) {
                            viewModel.updateUserName(newName)
                            dismiss()
                        } else {
                            binding.tilNewName.error =
                                resources.getString(R.string.same_current_new_name)
                        }
                    }
                    if (currentPassword.isNotBlank()) {
                        viewModel.reauthenticate(
                            currentPassword
                        ) { _, error ->
                            if (error == null) {
                                if (newEmail.isNotBlank()) {
                                    if (newEmail != user.email.toString()) {
                                        callback.verifyBeforeUpdateEmail(newEmail)
                                        dismiss()
                                    } else {
                                        binding.tilNewEmail.error =
                                            resources.getString(R.string.same_current_new_email)
                                    }
                                }
                                if (newPassword.isNotBlank()) {
                                    if (newPassword != currentPassword) {
                                        callback.updatePassword(newPassword)
                                        dismiss()
                                    } else {
                                        binding.tilNewPassword.error =
                                            resources.getString(R.string.same_current_new_password)
                                    }
                                }
                            } else {
                                binding.tilCurrentPassword.error =
                                    resources.getString(
                                        R.string.password_is_invalid
                                    )
                                binding.tietCurrentPassword.text = null

                                showErrorToast(requireContext(), error)
                            }
                        }
                    } else {
                        binding.tilCurrentPassword.error =
                            resources.getString(R.string.alert_enter_current_password)
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        resources.getString(R.string.specify_fields),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                callback.resetUser(user)
            }
            negativeButton.setOnClickListener {
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
        binding.tietNewName.addTextChangedListener {
            if (it?.toString()?.isNotBlank() == true) {
                binding.tilNewName.error = null
            }
        }
        binding.tietNewEmail.addTextChangedListener {
            if (it?.toString()?.isNotBlank() == true) {
                binding.tilNewEmail.error = null
            }
        }
        binding.tietNewPassword.addTextChangedListener {
            if (it?.toString()?.isNotBlank() == true) {
                binding.tilNewPassword.error = null
            }
        }
        binding.tietCurrentPassword.addTextChangedListener {
            if (it?.toString()?.isNotBlank() == true) {
                binding.tilCurrentPassword.error = null
            }
        }
        binding.tvResetPassword.setOnClickListener {
            callback.resetPassword()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditProfileDialogFragment"
    }
}