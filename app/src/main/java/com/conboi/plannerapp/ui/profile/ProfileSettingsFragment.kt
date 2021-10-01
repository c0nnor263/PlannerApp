package com.conboi.plannerapp.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.Slide
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentProfileSettingsBinding
import com.conboi.plannerapp.utils.hideKeyboard
import com.conboi.plannerapp.utils.popBackStackAllInstances
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ProfileSettingsFragment : Fragment() {
    private var _binding: FragmentProfileSettingsBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_profile_settings, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Slide().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_medium).toLong()
        }
        returnTransition = Slide().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_medium).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().bottom_floating_button.apply {
            setOnClickListener(null)
            setOnLongClickListener(null)
        }
        binding.lifecycleOwner = this
        binding.user = Firebase.auth.currentUser
        binding.viewModel = profileViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.updateTotalCompleted(profileViewModel.getTotalCompleted())
        }
        binding.apply {
            profileSettingsToolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
            profileSettingsUserId.setOnClickListener {
                val clipboard =
                    requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip: ClipData =
                    ClipData.newPlainText("User ID", profileSettingsUserId.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied User ID", Toast.LENGTH_SHORT).show()
            }
            profileSettingsBtnSignOut.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Warning!")
                    .setMessage("Are you sure want to sign out?")
                    .setPositiveButton("Yes") { dialog, _ ->
                        signOut()
                        dialog.dismiss()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()


            }
        }

    }

    private fun signOut() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        Firebase.auth.signOut()
        profileViewModel.signOutDelete()
        with(sharedPref.edit()) {
            putBoolean(com.conboi.plannerapp.ui.main.IMPORT_CONFIRM, false)
            apply()
        }
        findNavController().popBackStackAllInstances(
            findNavController().currentBackStackEntry?.destination?.id!!,
            true
        )
        findNavController().navigate(R.id.loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        _binding = null
    }
}