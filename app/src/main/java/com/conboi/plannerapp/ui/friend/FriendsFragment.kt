package com.conboi.plannerapp.ui.friend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.GridLayoutManager
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.FriendAdapter
import com.conboi.plannerapp.data.model.FriendType
import com.conboi.plannerapp.databinding.FragmentFriendsBinding
import com.conboi.plannerapp.interfaces.FriendListInterface
import com.conboi.plannerapp.interfaces.dialog.InviteFriendDialogCallback
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.utils.BaseTabFragment
import com.conboi.plannerapp.utils.hideAdView
import com.conboi.plannerapp.utils.shared.NoPreAnGridLayoutManager
import com.conboi.plannerapp.utils.showAdView
import com.conboi.plannerapp.utils.showErrorToast
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class FriendsFragment : BaseTabFragment(), FriendListInterface, InviteFriendDialogCallback {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FriendsViewModel by activityViewModels()

    private lateinit var mAdapterFriends: FriendAdapter

    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigate(R.id.mainFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_friends, container, false)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.sendCheckFriendListEvent()
        }
        (activity as MainActivity).binding.fabMain.apply {
            setOnClickListener {
                viewModel.sendInviteFriendEvent()
            }
            setOnLongClickListener(null)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        //Material Animation
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        view.background.alpha = 35

        initRv()

        viewModel.premiumState.observe(viewLifecycleOwner) {
            viewModel.sendGetPremiumEvent(it)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is FriendsViewModel.FriendsEvent.GetPremium -> {
                            if (state.alreadyGot) {
                                hideAdView(
                                    binding.adView,
                                    viewToPadding = binding.swipeRefresh,
                                    0
                                )
                            } else {
                                adView = showAdView(
                                    requireContext(),
                                    binding.adView,
                                    viewToPadding = binding.swipeRefresh,
                                    50
                                )
                            }
                        }
                        is FriendsViewModel.FriendsEvent.ShowRequestFriend -> {
                            showRequestFriend(state.id)
                            viewModel.sendRequestFriendEvent(null, null)
                        }
                        is FriendsViewModel.FriendsEvent.ShowFriendOptions -> {
                            showFriendOptions(state.view, state.id)
                            viewModel.sendFriendOptionsEvent(null, null, null)
                        }
                        is FriendsViewModel.FriendsEvent.NavigateDetails -> {
                            navigateToFriendDetails(state.friend)
                            viewModel.sendNavigateDetailsEvent(null, null)
                        }
                        FriendsViewModel.FriendsEvent.ShowInviteFriend -> {
                            inviteFriend()
                            viewModel.sendInviteFriendEvent(null)
                        }
                        FriendsViewModel.FriendsEvent.CheckFriendList -> {
                            checkFriendList()
                            viewModel.sendCheckFriendListEvent(null)
                        }
                        else -> {}
                    }
                }
            }
        }

        viewModel.sendCheckFriendListEvent()
    }


    override fun onClick(friend: FriendType) {
        when (friend.user_request_code) {
            1 -> viewModel.sendNavigateDetailsEvent(friend)
            2 -> viewModel.sendRequestFriendEvent(friend.user_id)
        }
    }

    override fun onHold(view: View, id: String) {
        viewModel.sendFriendOptionsEvent(view, id)
    }

    override fun successAddedFriend() {
        Toast.makeText(
            context,
            resources.getString(R.string.successfully_adding_friend),
            Toast.LENGTH_SHORT
        ).show()
        checkFriendList()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        adView = null
    }


    private fun initRv() {
        val config = PagingConfig(4, 12, false)
        val query = viewModel.getFriendQuery()
        val options = FirestorePagingOptions.Builder<FriendType>()
            .setLifecycleOwner(this)
            .setQuery(query, config, FriendType::class.java)
            .build()

        RxJavaPlugins.setErrorHandler { }
        mAdapterFriends = FriendAdapter(options, this)

        binding.rvFriends.apply {
            adapter = mAdapterFriends
            layoutManager =
                NoPreAnGridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mAdapterFriends.loadStateFlow.collectLatest { loadStates ->
                    _binding?.apply {
                        when (loadStates.refresh) {
                            is LoadState.Error -> {
                                swipeRefresh.isRefreshing = false
                                Toast.makeText(
                                    context,
                                    resources.getString(R.string.error_loading_friends),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is LoadState.NotLoading -> swipeRefresh.isRefreshing = false
                            else -> swipeRefresh.isRefreshing = false
                        }

                        when (loadStates.append) {
                            is LoadState.Error -> {
                                swipeRefresh.isRefreshing = false
                                Toast.makeText(
                                    context,
                                    resources.getString(R.string.there_are_no_more_friends_for_loading),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is LoadState.NotLoading -> swipeRefresh.isRefreshing = false
                            else -> swipeRefresh.isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    private fun checkFriendList() {
        viewModel.getFriendList { result, error ->
            if (error == null) {
                if (result?.isNotEmpty() == true) {
                    checkEveryFriendInfo(result)
                } else {
                    refreshAdapter()
                }
            } else {
                refreshAdapter()
                showErrorToast(requireContext(), error)
            }
        }
    }

    private fun checkEveryFriendInfo(friendList: List<DocumentSnapshot>) {
        viewModel.checkEveryFriendInfo(friendList) { _, error ->
            if (error == null) {
                refreshAdapter()
            } else {
                refreshAdapter()
                showErrorToast(requireContext(), error)
            }
        }
    }

    private fun inviteFriend() {
        val inviteFriendDialogFragment = InviteFriendDialogFragment(this)
        inviteFriendDialogFragment.show(parentFragmentManager, InviteFriendDialogFragment.TAG)
    }

    private fun showFriendOptions(view: View, id: String) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.popup_menu_friend_options, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_delete_friend -> {
                        viewModel.deleteFriend(id) { _, error ->
                            if (error == null) {
                                checkFriendList()
                                Toast.makeText(
                                    requireContext(),
                                    resources.getString(R.string.friend_removed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showErrorToast(requireContext(), error)
                            }
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun showRequestFriend(friendId: String): AlertDialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.you_add_friend))
            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                viewModel.addFriend(friendId) { _, error ->
                    if (error == null) {
                        checkFriendList()
                        dialog.dismiss()
                    } else {
                        showErrorToast(requireContext(), error)
                        dialog.dismiss()
                    }
                }
            }
            .setNegativeButton(resources.getString(R.string.deny)) { dialog, _ ->
                viewModel.denyFriendRequest(friendId)
                Toast.makeText(
                    context,
                    resources.getString(R.string.friend_request_deny),
                    Toast.LENGTH_SHORT
                ).show()

                refreshAdapter()
                dialog.dismiss()
            }.show()

    private fun navigateToFriendDetails(friend: FriendType) {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        val direction = FriendsFragmentDirections.actionFriendsFragmentToFriendDetailsFragment(
            friend = friend
        )
        findNavController().navigate(direction)
    }

    private fun refreshAdapter() {
        mAdapterFriends.refresh()
        lifecycleScope.launch {
            delay(300)
            if (_binding != null) {
                if (mAdapterFriends.itemCount == 0) {
                    binding.tvNoFriends.visibility = View.VISIBLE
                } else {
                    binding.tvNoFriends.visibility = View.INVISIBLE
                }
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}