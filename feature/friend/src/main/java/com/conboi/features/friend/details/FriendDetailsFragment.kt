package com.conboi.features.friend.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.conboi.core.data.showErrorToast
import com.conboi.core.domain.FRIENDS_TAG
import com.conboi.features.friend.R
import com.conboi.plannerapp.adapter.FriendTaskAdapter
import com.conboi.plannerapp.adapter.TaskTypeDiffCallback
import com.conboi.plannerapp.databinding.FragmentFriendDetailsBinding
import com.conboi.plannerapp.utils.getColorPrimaryTheme
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class FriendDetailsFragment : Fragment() {
    @Inject
    lateinit var diffCallback: TaskTypeDiffCallback

    private var _binding: FragmentFriendDetailsBinding? = null
    val binding get() = _binding!!

    private val viewModel: FriendDetailsViewModel by activityViewModels()

    private lateinit var mAdapterFriendTask: FriendTaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition =
            MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
                duration = 300.toLong()
            }
        returnTransition =
            MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
                duration = 300.toLong()
            }
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_friend_details, container, false)
        mAdapterFriendTask = FriendTaskAdapter(diffCallback)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.mBtnPrivate.setOnClickListener {
            viewModel.updatePrivateMode()
        }
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (abs(verticalOffset) == appBarLayout.totalScrollRange) {
                TransitionManager.beginDelayedTransition(
                    binding.root as CoordinatorLayout,
                    Fade(),
                )
                binding.ivAvatar.visibility = View.INVISIBLE
            } else if (verticalOffset <= -0.5) {
                TransitionManager.beginDelayedTransition(
                    binding.root as CoordinatorLayout,
                    Fade(),
                )
                binding.ivAvatar.visibility = View.VISIBLE
            }
        }
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        // Setting color theme
        lifecycleScope.launch {
            val colorPrimaryVariant = requireContext().getColorPrimaryTheme(FRIENDS_TAG)
            delay(100)
            binding.collapsingToolbar.setContentScrimResource(colorPrimaryVariant)
            binding.appBarLayout.setBackgroundResource(colorPrimaryVariant)
        }

        val navigationArgs: FriendDetailsFragmentArgs by navArgs()
        val friend = navigationArgs.friend
        binding.friend = friend

        binding.rvTasks.apply {
            adapter = mAdapterFriendTask
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        viewModel.setIndividualPrivateMode(friend.user_individual_private)

        viewModel.taskList.observe(this.viewLifecycleOwner) {
            mAdapterFriendTask.submitList(it)
        }
        viewModel.individualPrivateMode.observe(this.viewLifecycleOwner) { individualPrivateState ->
            viewModel.updatePrivateFriendServer(
                friend.user_id,
                individualPrivateState,
            )
            if (!friend.user_private_mode &&
                !individualPrivateState &&
                !friend.user_friend_private
            ) {
                binding.rvTasks.adapter = mAdapterFriendTask
                binding.rvTasks.visibility = View.VISIBLE
                downloadTasks(friend.user_id, friend.user_name)
            } else {
                binding.rvTasks.adapter = null
                binding.rvTasks.visibility = View.GONE
                binding.circularLoading.visibility = View.GONE

                binding.tvMsgEmpty.text =
                    resources.getString(R.string.friends_tasks_private, friend.user_name)
                binding.tvMsgEmpty.visibility = View.VISIBLE
            }
            privateModeButtonUI(individualPrivateState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveState()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewModel.restoreState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun privateModeButtonUI(privateModeState: Boolean) {
        if (privateModeState) {
            binding.mBtnPrivate.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.primaryDarkColorTree,
                ),
            )
            binding.mBtnPrivate.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_person_24)
        } else {
            binding.mBtnPrivate.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.primaryDarkColorAir,
                ),
            )
            binding.mBtnPrivate.icon =
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_person_outline_24,
                )
        }
    }

    private fun downloadTasks(
        id: String,
        name: String,
    ) {
        binding.circularLoading.visibility = View.VISIBLE

        viewModel.downloadFriendTaskList(id) { result, error ->
            if (error == null) {
                _binding?.let {
                    result?.let {
                        binding.circularLoading.visibility = View.GONE

                        viewModel.setTaskList(result)

                        val filter = result.filter { it.checked }
                        binding.tvCountOfTasks.text =
                            resources.getString(
                                R.string.count_of_tasks,
                                filter.size,
                                result.size,
                            )
                        binding.tvMsgEmpty.visibility = View.INVISIBLE
                    }
                }
            } else {
                _binding?.let {
                    binding.circularLoading.visibility = View.GONE

                    binding.tvMsgEmpty.text =
                        resources.getString(
                            R.string.there_will_be_shown_friends_tasks,
                            name,
                        )

                    binding.tvMsgEmpty.visibility = View.VISIBLE
                }
                Toast.makeText(
                    context,
                    resources.getString(R.string.friends_tasks_empty),
                    Toast.LENGTH_SHORT,
                ).show()
                showErrorToast(requireContext(), error)
            }
        }
    }
}
