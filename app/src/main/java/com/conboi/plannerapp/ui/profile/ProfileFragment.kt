package com.conboi.plannerapp.ui.profile

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.AlertdialogEditProfileBinding
import com.conboi.plannerapp.databinding.FragmentProfileBinding
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.myclass.AlarmMethods
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ProfileFragment : BaseTabFragment() {
    @Inject
    lateinit var alarmMethods: AlarmMethods

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    val auth = Firebase.auth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        return binding.root
    }


    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        binding.lifecycleOwner = this
        binding.bindUser = auth.currentUser!!
        binding.viewModel = sharedViewModel

        view.background.alpha = 80
        val user = auth.currentUser!!

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigate(R.id.mainFragment)
        }
        (activity as MainActivity).binding.bottomFloatingButton.apply {
            setOnClickListener { editProfile(user) }
            setOnLongClickListener(null)
        }



        binding.apply {
            if (!user.isEmailVerified) {
                userConfirmEmail.setTextColor(Color.RED)
                userConfirmEmail.visibility = View.VISIBLE
            }
            userId.setOnClickListener {
                val clipboard =
                    requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip: ClipData =
                    ClipData.newPlainText(
                        resources.getString(R.string.user_id),
                        userId.text
                    )
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    context,
                    resources.getString(R.string.copied_user_id),
                    Toast.LENGTH_SHORT
                ).show()
            }
            lifecycleScope.launch {
                delay(100)
                btnSignOut.supportBackgroundTintList = ContextCompat.getColorStateList(
                    requireContext(),
                    sharedViewModel.colorPrimaryVariant.value!!
                )
            }
            btnSignOut.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(resources.getString(R.string.warning))
                    .setMessage(resources.getString(R.string.you_sign_out))
                    .setPositiveButton(resources.getString(R.string.submit)) { dialog, _ ->
                        signOut(true)
                        dialog.dismiss()
                    }
                    .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()


            }
            userConfirmEmail.setOnClickListener {
                if (!user.isEmailVerified) {
                    auth.useAppLanguage()
                    user.sendEmailVerification().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                requireContext(),
                                resources.getString(R.string.you_sent_email_confirm),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                resources.getString(R.string.cannot_sent_email_confirm),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

    }

    private fun editProfile(user: FirebaseUser) {
        var _editProfileBinding: AlertdialogEditProfileBinding? =
            AlertdialogEditProfileBinding.inflate(layoutInflater)
        val editProfileBinding = _editProfileBinding!!
        editProfileBinding.apply {
            newName.addTextChangedListener {
                if (it?.toString()?.isNotBlank() == true) {
                    editProfileBinding.newNameLayout.error = null
                }
            }
            newEmail.addTextChangedListener {
                if (it?.toString()?.isNotBlank() == true) {
                    editProfileBinding.newEmailLayout.error = null
                }
            }
            newPassword.addTextChangedListener {
                if (it?.toString()?.isNotBlank() == true) {
                    editProfileBinding.newPasswordLayout.error = null
                }
            }
            currentPassword.addTextChangedListener {
                if (it?.toString()?.isNotBlank() == true) {
                    editProfileBinding.currentPasswordLayout.error = null
                }
            }

        }
        val profileDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(editProfileBinding.root)
            .setTitle(resources.getString(R.string.edit_profile))
            .setCancelable(false)
            .setPositiveButton(resources.getString(R.string.save), null)
            .setNegativeButton(resources.getString(R.string.cancel), null)
            .create()
        run positiveButton@{
            profileDialog.setOnShowListener { dialog ->
                val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                positiveButton.setOnClickListener {
                    val newName = editProfileBinding.newName.text.toString()
                    val newEmail = editProfileBinding.newEmail.text.toString().trim()
                    val newPassword = editProfileBinding.newPassword.text.toString()
                    val currentPassword = editProfileBinding.currentPassword.text.toString()

                    if (newName.isNotBlank() || newEmail.isNotBlank() || newPassword.isNotBlank()) {
                        if (newName.isNotBlank()) {
                            if (newName != user.displayName.toString()) {
                                user.updateProfile(userProfileChangeRequest {
                                    displayName = newName
                                }).addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        Toast.makeText(
                                            requireContext(),
                                            resources.getString(R.string.name_updated),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        user.reload()
                                        binding.bindUser = user
                                        _editProfileBinding = null
                                        dialog.dismiss()
                                        return@addOnCompleteListener
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            resources.getString(R.string.error_name_update),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                editProfileBinding.newNameLayout.error =
                                    resources.getString(R.string.same_new_old_name)
                            }
                        }
                        if (currentPassword.isNotBlank()) {
                            user.reauthenticate(
                                EmailAuthProvider.getCredential(
                                    user.email.toString(),
                                    currentPassword
                                )
                            ).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    if (newEmail.isNotBlank()) {
                                        if (newEmail != user.email.toString()) {
                                            user.verifyBeforeUpdateEmail(newEmail)
                                                .addOnCompleteListener {
                                                    if (it.isSuccessful) {
                                                        Toast.makeText(
                                                            requireContext(),
                                                            resources.getString(R.string.check_new_email),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        _editProfileBinding = null
                                                        signOut(false)
                                                        dialog.dismiss()
                                                    } else {
                                                        Toast.makeText(
                                                            requireContext(),
                                                            resources.getString(R.string.error_check_new_email),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                        } else {
                                            editProfileBinding.newEmailLayout.error =
                                                resources.getString(R.string.same_new_old_email)
                                        }
                                    }
                                    if (newPassword.isNotBlank()) {
                                        if (newPassword != currentPassword) {
                                            user.updatePassword(newPassword)
                                                .addOnCompleteListener {
                                                    if (it.isSuccessful) {
                                                        Toast.makeText(
                                                            requireContext(),
                                                            resources.getString(R.string.successfully_change_password),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        signOut(false)
                                                        _editProfileBinding = null
                                                        dialog.dismiss()
                                                    } else {
                                                        Toast.makeText(
                                                            requireContext(),
                                                            resources.getString(R.string.error_check_new_password),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                        } else {
                                            editProfileBinding.newPasswordLayout.error =
                                                resources.getString(R.string.same_new_old_password)
                                        }
                                    }
                                } else {
                                    editProfileBinding.currentPasswordLayout.error =
                                        resources.getString(
                                            R.string.unsuccessful_sign_in_current_password
                                        )
                                    editProfileBinding.currentPassword.text = null
                                }
                            }
                        } else {
                            if (newName.isBlank() || newEmail.isNotBlank() || newPassword.isNotBlank()) {
                                editProfileBinding.currentPasswordLayout.error =
                                    resources.getString(R.string.alert_enter_current_password)
                            }

                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.specify_fields),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    user.reload()
                    binding.bindUser = user
                }
                negativeButton.setOnClickListener {
                    _editProfileBinding = null
                    dialog.cancel()
                }
            }
        }
        profileDialog.show()

    }

    private fun signOut(navigateToLogin: Boolean) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        auth.signOut()
        sharedViewModel.signOutDelete()
        ContextCompat.getSystemService(requireContext(), NotificationManager::class.java)
            ?.cancelAll()
        alarmMethods.cancelAllAlarmsType(requireContext(), null)
        with(sharedPref.edit()) {
            putBoolean(IMPORT_CONFIRM, false)
            putBoolean(EMAIL_CONFIRM, false)
            apply()
        }
        if (navigateToLogin) {
            findNavController().popBackStackAllInstances(
                findNavController().currentBackStackEntry?.destination?.id!!,
                true
            )
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        _binding = null
    }
}