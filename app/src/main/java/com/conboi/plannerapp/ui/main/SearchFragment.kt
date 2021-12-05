package com.conboi.plannerapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.SearchTaskAdapter
import com.conboi.plannerapp.databinding.FragmentSearchBinding
import com.conboi.plannerapp.ui.MainActivity
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        (activity as MainActivity).binding.bottomAppBar.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Out enter
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }

        //Enter
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        val mAdapter = SearchTaskAdapter()

        binding.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            binding.etSearch.apply {
                addTextChangedListener { text ->
                    if (text.toString().isNotBlank()) {
                        viewModel.searchQuery.value = text.toString()
                    }
                }
                if (viewModel.searchQuery.value!!.isNotEmpty()) {
                    setText(viewModel.searchQuery.value)
                }
            }

            rvTasks.apply {
                adapter = mAdapter
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
            }

        }


        viewModel.sortedTasks.observe(viewLifecycleOwner) { items ->
            items.let {
                if (binding.etSearch.text?.isNotBlank() == true) {
                mAdapter.submitList(it)
            }
                if (it.isNotEmpty()) {
                    binding.tvNoTasks.visibility = View.GONE
                } else {
                    binding.tvNoTasks.visibility = View.VISIBLE
                }
            }
        }

    }

    override fun onStop() {
        super.onStop()
        (activity as MainActivity).binding.bottomAppBar.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}