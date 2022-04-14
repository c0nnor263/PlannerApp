package com.conboi.plannerapp.ui.profile

import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentProfileBinding
import com.conboi.plannerapp.interfaces.dialog.EditProfileDialogCallback
import com.conboi.plannerapp.ui.IntroActivity
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.shared.LoadingDialogFragment
import com.google.android.gms.ads.AdView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.auth.FirebaseUser
import com.qonversion.android.sdk.Qonversion
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val colorTintList: (Context, Int) -> ColorStateList? = { context, colorId ->
    ContextCompat.getColorStateList(context, colorId)
}

@AndroidEntryPoint
class ProfileFragment : BaseTabFragment(), EditProfileDialogCallback {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private var adView: AdView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)

        (activity as MainActivity).let { activity ->
            activity.binding.fabMain.setOnClickListener {
                showEditProfile()
            }
            activity.binding.fabMain.setOnLongClickListener(null)
            activity.onBackPressedDispatcher.addCallback(this) {
                findNavController().navigate(R.id.mainFragment)
            }
        }
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.bindUser = viewModel.user
        binding.viewModel = viewModel

        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        view.background.alpha = 80


        binding.mBtnSignOut.setOnClickListener {
            showSignOutDialog()
        }

        binding.tvId.setOnClickListener {
            copyUserID()
        }

        binding.tvConfirmEmail.setOnClickListener {
            viewModel.sendConfirmationEmail { _, error ->
                if (error == null) {
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
                    showErrorToast(requireContext(), error)
                }
            }
        }

        binding.mBtnTutorial.setOnClickListener {
            val intent = Intent(requireContext(), IntroActivity::class.java)
            startActivity(intent)
        }
        binding.tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()

        val isEmailConfirmed = viewModel.user?.isEmailVerified
        binding.mBtnUpgrade.setOnClickListener {
            if (isEmailConfirmed == true) {
                navigateToSubscribeFragment()
            } else {
                showEmailNotConfirmedDialog()
            }
        }
        if (isEmailConfirmed == false) {
            binding.tvConfirmEmail.setTextColor(Color.RED)
            binding.tvConfirmEmail.visibility = View.VISIBLE
        }


        viewModel.premiumState.observe(viewLifecycleOwner) {
            if (it) {
                hideAdView(binding.adView, viewToPadding = binding.parentSv, 0)
                binding.mBtnUpgrade.toSuccess()
            } else {
                adView = showAdView(requireContext(), binding.adView, binding.parentSv, 55)
                binding.mBtnUpgrade.toStandard()
            }
        }

        // Color theme changing
        lifecycleScope.launch {
            val colorTheme = requireContext().getColorPrimaryTheme(PROFILE_TAG)
            delay(100)
            ViewCompat.setBackgroundTintList(
                binding.mBtnSignOut,
                colorTintList(
                    requireContext(),
                    colorTheme
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.user != null) {
            viewModel.reloadUser()
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
        adView = null
        _binding = null
    }


    override fun resetUser(user: FirebaseUser) {
        viewModel.reloadUser()
        binding.bindUser = user
    }

    override fun verifyBeforeUpdateEmail(newEmail: String) {
        viewModel.verifyBeforeUpdateEmail(newEmail) { _, verifyError ->
            if (verifyError == null) {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.check_new_email),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.error_check_new_email),
                    Toast.LENGTH_SHORT
                ).show()
                showErrorToast(requireContext(), verifyError)
            }
        }
    }

    override fun updatePassword(newPassword: String) {
        viewModel.updatePassword(newPassword) { _, updatePasswordError ->
            if (updatePasswordError == null) {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.successfully_change_password),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.error_check_new_password),
                    Toast.LENGTH_SHORT
                ).show()
                showErrorToast(
                    requireContext(),
                    updatePasswordError
                )
            }
        }
    }

    override fun resetPassword() {
        viewModel.resetPassword { _, error ->
            if (error == null) {
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
                showErrorToast(requireContext(), error)
            }
        }
    }


    private fun copyUserID() {
        val clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData =
            ClipData.newPlainText(
                resources.getString(R.string.user_id),
                binding.tvId.text
            )
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            resources.getString(R.string.copied_user_id),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showEmailNotConfirmedDialog(): AlertDialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.warning))
            .setMessage(resources.getString(R.string.email_not_confirmed))
            .setNeutralButton(resources.getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }.show()


    private fun navigateToSubscribeFragment() {
        findNavController().currentDestination?.apply {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            }
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            }
        }
        findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToSubscribeFragment())
    }

    private fun showSignOutDialog(): AlertDialog = MaterialAlertDialogBuilder(requireContext())
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

    private fun showEditProfile() {
        val editProfileFragment = EditProfileDialogFragment(this)
        editProfileFragment.show(parentFragmentManager, EditProfileDialogFragment.TAG)
    }

    private fun signOut() {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.isCancelable = false
        loadingDialog.show(
            parentFragmentManager, LoadingDialogFragment.TAG
        )

        viewModel.sortedTasks.observe(viewLifecycleOwner) { currentList ->
            if (currentList.isNullOrEmpty()) {
                successOut()
                loadingDialog.dismiss()
                return@observe
            }

            viewModel.signOutUploadTasks(currentList) { _, error ->
                if (error == null) {
                    successOut()
                    loadingDialog.dismiss()
                } else {
                    showErrorToast(requireContext(), error)
                    loadingDialog.dismiss()
                }
            }
        }
    }

    private fun successOut() {
        viewModel.signOut()
        ContextCompat.getSystemService(requireContext(), NotificationManager::class.java)
            ?.cancelAll()
        Qonversion.logout()
    }

    private fun MaterialButton.toSuccess() {
        binding.mBtnUpgrade.text = resources.getString(R.string.max)
        lifecycleScope.launch {
            delay(100)

            ViewCompat.setBackgroundTintList(
                binding.mBtnUpgrade, colorTintList(
                    requireContext(),
                    R.color.primaryColorTree
                )
            )
        }
    }

    private fun MaterialButton.toStandard() {
        binding.mBtnUpgrade.text = resources.getString(R.string.upgrade)
        lifecycleScope.launch {
            delay(100)
            ViewCompat.setBackgroundTintList(
                binding.mBtnUpgrade, colorTintList(
                    requireContext(),
                    R.color.primaryColorFire
                )
            )
        }
    }
}