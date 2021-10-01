package com.conboi.plannerapp.ui

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
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_bottom_navigation.*


class BottomNavigationDrawerFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentBottomNavigationBinding? = null
    val binding get() = _binding!!

    //Firebase
    private var auth: FirebaseAuth = Firebase.auth

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
        NavigationUI.setupWithNavController(binding.bottomNavigationView, findNavController())
        binding.apply {
            lifecycleOwner = this@BottomNavigationDrawerFragment
            user = auth.currentUser
            bottomNavigationView.setNavigationItemSelectedListener { menuItem ->
                val id = menuItem.itemId
                if (menuItem.isChecked) return@setNavigationItemSelectedListener false
                when (id) {
                    R.id.bottom_navigation_menu_water_fragment -> {
                        if (findNavController().currentDestination?.id == R.id.mainFragment) {
                            dismiss()
                            return@setNavigationItemSelectedListener false
                        }
                        requireActivity().bottom_app_bar_count_of_tasks.visibility = View.VISIBLE
                        findNavController().navigate(R.id.mainFragment)
                        dismiss()
                    }
                    R.id.bottom_navigation_menu_fire_fragment -> {
                        if (findNavController().currentDestination?.id == R.id.friendsFragment) {
                            dismiss()
                            return@setNavigationItemSelectedListener false
                        }
                        requireActivity().bottom_app_bar_count_of_tasks.visibility = View.GONE
                        requireActivity().bottom_app_bar_count_of_completed.visibility = View.GONE
                        findNavController().navigate(R.id.friendsFragment)
                        dismiss()
                    }
                    R.id.bottom_navigation_menu_tree_fragment -> {
                        if (findNavController().currentDestination?.id == R.id.settingsFragment) {
                            dismiss()
                            return@setNavigationItemSelectedListener false
                        }
                        requireActivity().bottom_app_bar_count_of_tasks.visibility = View.GONE
                        requireActivity().bottom_app_bar_count_of_completed.visibility = View.GONE
                        findNavController().navigate(R.id.settingsFragment)
                        dismiss()
                    }
                }
                true
            }
            val navigationMenuHeader = bottomNavigationView.getChildAt(0) as NavigationMenuView
            navigationMenuHeader.isVerticalScrollBarEnabled = false
            bottomNavigationProfileSettings.setOnClickListener {
                if (findNavController().currentDestination?.id == R.id.profileSettingsFragment) {
                    dismiss()
                    return@setOnClickListener
                }
                findNavController().navigate(R.id.profileSettingsFragment)
                dismiss()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
