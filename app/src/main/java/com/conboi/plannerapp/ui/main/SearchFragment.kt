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
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
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
            fragmentSearchToolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            binding.fragmentSearchEdittext.apply {
                addTextChangedListener { text ->
                    if (text.toString().isNotBlank()) {
                        viewModel.searchQuery.value = text.toString()
                    }
                }
                if (viewModel.searchQuery.value!!.isNotEmpty()) {
                    setText(viewModel.searchQuery.value)
                }
            }

            fragmentSearchRvTasks.apply {
                adapter = mAdapter
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                setOnClickListener {
                    requireActivity().bottom_floating_button.show()
                }
            }

        }


        viewModel.allTasks.observe(viewLifecycleOwner) { items ->
            items.let {
                if (binding.fragmentSearchEdittext.text.isNotBlank()) {
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}