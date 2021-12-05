package com.conboi.plannerapp.ui.bottomsheet

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentBottomNavigationBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.internal.NavigationMenuView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class BottomNavigationFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentBottomNavigationBinding? = null
    val binding get() = _binding!!

    //Firebase
    private val auth: FirebaseAuth = Firebase.auth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_bottom_navigation, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NavigationUI.setupWithNavController(binding.appNavigationView, findNavController())
        binding.apply {
            lifecycleOwner = this@BottomNavigationFragment
            user = auth.currentUser
            if (!auth.currentUser?.isEmailVerified!!) {
                profileEmail.setTextColor(Color.RED)
            }
            appNavigationView.setNavigationItemSelectedListener { menuItem ->
                val id = menuItem.itemId
                if (menuItem.isChecked) return@setNavigationItemSelectedListener false
                when (id) {
                    R.id.bottom_navigation_menu_main_fragment -> {
                        if (findNavController().currentDestination?.id == R.id.mainFragment) {
                            dismiss()
                            return@setNavigationItemSelectedListener false
                        }
                        findNavController().navigate(R.id.mainFragment)
                        dismiss()
                    }
                    R.id.bottom_navigation_menu_friends_fragment -> {
                        if (findNavController().currentDestination?.id == R.id.friendsFragment) {
                            dismiss()
                            return@setNavigationItemSelectedListener false
                        }
                        findNavController().navigate(R.id.friendsFragment)
                        dismiss()
                    }
                    R.id.bottom_navigation_menu_profile_fragment -> {
                        if (findNavController().currentDestination?.id == R.id.profileFragment) {
                            dismiss()
                            return@setNavigationItemSelectedListener false
                        }
                        findNavController().navigate(R.id.profileFragment)
                        dismiss()
                    }
                }
                true
            }
            val navigationMenuHeader = appNavigationView.getChildAt(0) as NavigationMenuView
            navigationMenuHeader.isVerticalScrollBarEnabled = false
            imBtnProfileSettings.setOnClickListener {
                if (findNavController().currentDestination?.id == R.id.settingsFragment) {
                    dismiss()
                    return@setOnClickListener
                }
                val settingsFragment = SettingsFragment()
                settingsFragment.show(
                    requireActivity().supportFragmentManager,
                    settingsFragment.tag
                )
                dismiss()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
