package com.conboi.plannerapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.conboi.plannerapp.ui.auth.options.SignInFragment
import com.conboi.plannerapp.ui.auth.options.SignUpFragment

class LoginViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount() = 2


    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SignInFragment()
            1 -> SignUpFragment()
            else -> SignInFragment()
        }
    }
}