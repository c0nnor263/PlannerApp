package com.conboi.plannerapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentSettingsBinding
import com.conboi.plannerapp.utils.BaseTabFragment
import com.conboi.plannerapp.utils.hideKeyboard
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class SettingsFragment : BaseTabFragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigate(R.id.mainFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().bottom_floating_button.apply {
            setOnClickListener(null)
            setOnLongClickListener(null)
        }
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        view.background.alpha = 35
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        _binding = null
    }
}