package com.conboi.plannerapp.ui.main.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.SearchTaskAdapter
import com.conboi.plannerapp.adapter.TaskTypeDiffCallback
import com.conboi.plannerapp.databinding.FragmentSearchBinding
import com.conboi.plannerapp.interfaces.ListInterface
import com.conboi.plannerapp.ui.MainActivity
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment(), ListInterface {
    @Inject
    lateinit var diffCallback: TaskTypeDiffCallback

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        postponeEnterTransition()

        val mAdapter = SearchTaskAdapter(this, diffCallback)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.etSearch.addTextChangedListener { text ->
            if (text.toString().isNotBlank()) {
                viewModel.searchQuery.value = text.toString()
            }
        }

        binding.rvTasks.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        viewModel.sortedTasks.observe(this.viewLifecycleOwner) { items ->
            items.let {
                if (binding.etSearch.text?.isNotBlank() == true) {
                    mAdapter.submitList(it)
                }
                binding.tvNoTasks.visibility = if (it.isNotEmpty()) View.GONE else View.VISIBLE
                view.doOnPreDraw { startPostponedEnterTransition() }
            }
        }

        if (viewModel.searchQuery.value?.isNotEmpty() == true) {
            binding.etSearch.setText(viewModel.searchQuery.value)
        }
    }


    override fun onClick(view: View, id: Int) = navigateToTaskDetail(id, view)

    override fun onHold() {}

    override fun onStop() {
        super.onStop()
        (activity as MainActivity).binding.bottomAppBar.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun navigateToTaskDetail(id: Int, view: View) {
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        val transitionName = resources.getString(R.string.task_detail_transition_name)
        val extras = FragmentNavigatorExtras(view to transitionName)
        val directions =
            SearchFragmentDirections.actionSearchFragmentToTaskDetailsFragment(
                idTask = id
            )
        findNavController().navigate(directions, extras)
    }
}