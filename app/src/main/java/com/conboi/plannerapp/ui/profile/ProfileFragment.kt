package com.conboi.plannerapp.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentProfileBinding
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.main.IMPORT_CONFIRM
import com.conboi.plannerapp.utils.BaseTabFragment
import com.conboi.plannerapp.utils.REMINDERS
import com.conboi.plannerapp.utils.hideKeyboard
import com.conboi.plannerapp.utils.popBackStackAllInstances
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ProfileFragment : BaseTabFragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.background.alpha = 80
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigate(R.id.mainFragment)
        }
        (activity as MainActivity).binding.bottomFloatingButton.apply {
            setOnClickListener(null)
            setOnLongClickListener(null)
        }

        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }

        binding.lifecycleOwner = this
        binding.user = Firebase.auth.currentUser
        binding.viewModel = profileViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.updateTotalCompleted(profileViewModel.getTotalCompleted())
        }
        binding.apply {
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
        }

    }

    private fun signOut() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val remindersPref =
            activity?.getSharedPreferences(REMINDERS, Context.MODE_PRIVATE) ?: return
        Firebase.auth.signOut()
        profileViewModel.signOutDelete()
        with(sharedPref.edit()) {
            putBoolean(IMPORT_CONFIRM, false)
            apply()
        }
        findNavController().popBackStackAllInstances(
            findNavController().currentBackStackEntry?.destination?.id!!,
            true
        )
        with(remindersPref.edit()) { clear() }
        findNavController().navigate(R.id.loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        _binding = null
    }
}