package com.conboi.plannerapp.ui.profile

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.AlertdialogEditProfileBinding
import com.conboi.plannerapp.databinding.FragmentProfileBinding
import com.conboi.plannerapp.ui.IntroActivity
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.main.LoadingAlertFragment
import com.conboi.plannerapp.ui.main.MainFragment
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.myclass.AlarmMethods
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.qonversion.android.sdk.Qonversion
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

    private var adView: AdView? = null
    val auth = Firebase.auth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        (requireActivity() as MainActivity).checkPermissions()
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
                        signOut()
                        dialog.dismiss()
                    }
                    .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()


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
                                resources.getString(R.string.error_send_email_confirm),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            upgradeSubscription.setOnClickListener {
                if (user.isEmailVerified) {
                    findNavController().currentDestination?.apply {
                        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                            duration =
                                resources.getInteger(R.integer.reply_motion_duration_large).toLong()
                        }
                        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                            duration =
                                resources.getInteger(R.integer.reply_motion_duration_large).toLong()
                        }
                    }
                    findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToSubscribeFragment())
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(resources.getString(R.string.warning))
                        .setMessage(resources.getString(R.string.email_not_confirmed))
                        .setNeutralButton(resources.getString(R.string.ok)) { dialog, _ ->
                            dialog.dismiss()
                        }
                }
            }
            tutorial.setOnClickListener {
                val intent = Intent(requireContext(), IntroActivity::class.java)
                startActivity(intent)
            }
            privacyPolicy.movementMethod = LinkMovementMethod.getInstance()
        }

        sharedViewModel.premiumState.observe(this.viewLifecycleOwner) {
            if (it) {
                binding.adView.visibility = View.GONE
                binding.upgradeSubscription.toSuccess()
                binding.parentScrollview.updatePadding(top = 0)

                adView?.destroy()
                adView = null
            } else {
                binding.adView.visibility = View.VISIBLE
                binding.upgradeSubscription.toStandard()
                val scale = resources.displayMetrics.density
                binding.parentScrollview.updatePadding(top = (55 * scale + 0.5f).toInt())

                adView = binding.adView
                adView?.loadAd(AdRequest.Builder().build())
            }
        }
    }

    private fun editProfile(user: FirebaseUser) {
        val editProfileBinding: AlertdialogEditProfileBinding =
            AlertdialogEditProfileBinding.inflate(layoutInflater)
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
            resetPassword.setOnClickListener {
                auth.sendPasswordResetEmail(user.email.toString()).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.password_reset_sent),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.password_reset_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
                                        resources.getString(R.string.nickname_updated),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    user.reload()
                                    binding.bindUser = user
                                    dialog.dismiss()
                                    return@addOnCompleteListener
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        resources.getString(R.string.error_nickname_update),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            editProfileBinding.newNameLayout.error =
                                resources.getString(R.string.same_current_new_name)
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
                                                    signOut()
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
                                            resources.getString(R.string.same_current_new_email)
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
                                                    signOut()
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
                                            resources.getString(R.string.same_current_new_password)
                                    }
                                }
                            } else {
                                editProfileBinding.currentPasswordLayout.error =
                                    resources.getString(
                                        R.string.password_is_invalid
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
                dialog.cancel()
            }
        }
        profileDialog.show()
    }

    private fun signOut() {
        val loadingDialog = LoadingAlertFragment()
        loadingDialog.show(
            childFragmentManager, LoadingAlertFragment.TAG
        )
        val activity = requireActivity() as MainActivity
        val currentList = sharedViewModel.allTasks.value
        if (currentList == null) {
            successOut()
            loadingDialog.dismiss()
            return
        }

        activity.db.document("Users/${activity.auth.currentUser?.uid}/TaskList/BackupTasks").set(
            currentList.associateBy(
                { (it.idTask + it.created).toString() },
                { it })
        ).addOnCompleteListener {backupList ->
            if (backupList.isSuccessful) {
                activity.db.document("Users/${activity.auth.currentUser?.uid}")
                    .update(MainFragment.KEY_USER_LAST_SYNC, System.currentTimeMillis()).addOnCompleteListener {lastSync ->
                        if(lastSync.isSuccessful){
                            successOut()
                            loadingDialog.dismiss()
                        }
                    }

            } else {
                Toast.makeText(activity, backupList.exception.toString(), Toast.LENGTH_SHORT).show()
                loadingDialog.dismiss()
            }
        }
    }

    private fun successOut() {
        val sharedPref = activity?.getSharedPreferences(APP_FILE, Context.MODE_PRIVATE) ?: return
        ContextCompat.getSystemService(requireContext(), NotificationManager::class.java)
            ?.cancelAll()
        AuthUI.getInstance().signOut(requireContext())
        Qonversion.logout()
        sharedViewModel.signOutDelete()
        alarmMethods.cancelAllAlarmsType(requireContext(), null)
        sharedPref.edit().clear().apply()
    }


    @SuppressLint("RestrictedApi")
    private fun MaterialButton.toSuccess() {
        binding.upgradeSubscription.text = resources.getString(R.string.max)
        lifecycleScope.launch {
            delay(100)
            binding.upgradeSubscription.supportBackgroundTintList = ContextCompat.getColorStateList(
                requireContext(),
                R.color.primaryColorTree
            )
        }
    }

    @SuppressLint("RestrictedApi")
    private fun MaterialButton.toStandard() {
        binding.upgradeSubscription.text = resources.getString(R.string.upgrade)
        lifecycleScope.launch {
            delay(100)
            binding.upgradeSubscription.supportBackgroundTintList = ContextCompat.getColorStateList(
                requireContext(),
                R.color.primaryColorFire
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (Firebase.auth.currentUser != null) {
            Firebase.auth.currentUser!!.reload()
        }
        adView?.resume()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        adView = null
        _binding = null
    }
}