package com.conboi.plannerapp.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.*
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.FriendAdapter
import com.conboi.plannerapp.databinding.FragmentFriendsBinding
import com.conboi.plannerapp.model.FriendType
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.main.MainFragment
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.BaseTabFragment
import com.conboi.plannerapp.utils.hideKeyboard
import com.conboi.plannerapp.utils.myclass.NoPreAnGridLayoutManager
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class FriendsFragment : BaseTabFragment(), FriendAdapter.OnFriendListInterface {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by viewModels()

    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    //Adapter for friend list
    private val query = FirebaseFirestore.getInstance()
        .collection("Users/${auth.currentUser!!.uid}/FriendList")
    private val config =
        PagingConfig(4, 12, false)
    private val options = FirestorePagingOptions.Builder<FriendType>()
        .setLifecycleOwner(this)
        .setQuery(query, config, FriendType::class.java)
        .build()

    private val mAdapterFriends: FriendAdapter = FriendAdapter(options, this)

    private var currentUserFriendList: List<DocumentSnapshot>? = null
    private var totalCompleted = 0

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkUserFriendList()
        viewLifecycleOwner.lifecycleScope.launch {
            totalCompleted =
                viewModel.preferencesFlow.first().totalCompleted
        }

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

        (activity as MainActivity).binding.bottomFloatingButton.apply {
            setOnClickListener {
               inviteFriend()
            }
            setOnLongClickListener(null)
        }

        //Friends RV
        binding.rvFriends.apply {
            adapter = mAdapterFriends
            layoutManager =
                NoPreAnGridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
        }

        binding.swiperefresh.setOnRefreshListener {
            mAdapterFriends.refresh()
            checkUserFriendList()
        }
        lifecycleScope.launch {
            mAdapterFriends.loadStateFlow.collectLatest { loadStates ->
                when (loadStates.refresh) {
                    is LoadState.Error -> {
                        binding.tvNoFriends.visibility = View.VISIBLE
                        binding.swiperefresh.isRefreshing = false
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.error_loading_friends),
                            Toast.LENGTH_SHORT
                        ).show()
                        // The initial load failed. Call the retry() method
                        // in order to retry the load operation.
                        // ...
                    }
                    is LoadState.NotLoading -> {
                        binding.swiperefresh.isRefreshing = false
                    }
                    else -> {}

                }
                when (loadStates.append) {
                    is LoadState.Error -> {
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.there_are_no_more_friends_for_loading),
                            Toast.LENGTH_SHORT
                        ).show()
                        // The additional load failed. Call the retry() method
                        // in order to retry the load operation.
                        // ...
                    }
                    is LoadState.NotLoading -> {
                        if (loadStates.refresh is LoadState.NotLoading) {
                            binding.swiperefresh.isRefreshing = false
                            // The previous load (either initial or additional) completed
                            // ...
                        }
                    }
                    else -> {}
                }
            }
        }

        val helper: SnapHelper = LinearSnapHelper()
        helper.attachToRecyclerView(binding.rvFriends)
    }

    private fun checkUserFriendList() {
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            db.collection("Users/${auth.currentUser!!.uid}/FriendList")
                .get()
                .addOnSuccessListener { friendsCollection ->
                    if (!friendsCollection.isEmpty) {
                        currentUserFriendList = friendsCollection.documents
                        checkFriendInfo()
                    } else {
                        binding.tvNoFriends.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener {
                    if (mAdapterFriends.itemCount == 0) {
                        binding.tvNoFriends.visibility = View.VISIBLE
                    }
                    binding.swiperefresh.isRefreshing = false
                    Toast.makeText(
                        context,
                        resources.getString(R.string.cant_get_user_friends, it.toString()),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun checkFriendInfo() {
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            //Getting current user friend list
            db.collection("Users")
                .get()
                .addOnCompleteListener { userCollection ->
                    //Get all users in Firestore
                    if (userCollection.isSuccessful) {
                        if (currentUserFriendList == null) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.cant_get_user_friends, ""),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addOnCompleteListener
                        }

                        val usersList: MutableList<QueryDocumentSnapshot> =
                            ArrayList()
                        for (document in userCollection.result) {
                            usersList.add(document)
                        }
                        //Searching a friend
                        for (friendDocument in currentUserFriendList!!) {
                            for (userDocument in usersList) {
                                if (friendDocument.getString(MainFragment.KEY_USER_ID) == userDocument.getString(
                                        MainFragment.KEY_USER_ID
                                    )
                                ) {
                                    //Get a friend info
                                    val friendMap: MutableMap<String, Any> =
                                        HashMap()
                                    friendMap[MainFragment.KEY_USER_ID] =
                                        userDocument.getString(
                                            MainFragment.KEY_USER_ID
                                        )
                                            .toString()
                                    friendMap[MainFragment.KEY_USER_PHOTO_URL] =
                                        userDocument.getString(
                                            MainFragment.KEY_USER_PHOTO_URL
                                        )
                                            .toString()
                                    friendMap[MainFragment.KEY_USER_NAME] =
                                        userDocument.getString(
                                            MainFragment.KEY_USER_NAME
                                        )
                                            .toString()
                                    friendMap[MainFragment.KEY_USER_EMAIL] =
                                        userDocument.getString(
                                            MainFragment.KEY_USER_EMAIL
                                        ).toString()
                                    friendMap[MainFragment.KEY_USER_COUNT_COMPLETED_TASKS] =
                                        userDocument.getLong(
                                            MainFragment.KEY_USER_COUNT_COMPLETED_TASKS
                                        )
                                            ?: 0
                                    db.document("Users/${auth.currentUser!!.uid}/FriendList/${friendMap[MainFragment.KEY_USER_ID]}")
                                        .set(friendMap, SetOptions.merge())
                                    if (_binding != null) {
                                        binding.tvNoFriends.visibility = View.GONE
                                        binding.swiperefresh.isRefreshing = false
                                    }

                                }
                            }
                        }
                    }
                }
        }
    }

    private fun promptRequestFriend(friendId: String) {
        //Adding new friend
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.you_add_friend))
            .setPositiveButton(resources.getString(R.string.confirm)) { dialog, _ ->
                val map: MutableMap<String, Any> = HashMap()
                map[MainFragment.KEY_USER_REQUEST] = 1
                db.document("Users/${friendId}/FriendList/${auth.currentUser!!.uid}")
                    .update(map)
                db.document("Users/${auth.currentUser!!.uid}/FriendList/${friendId}")
                    .update(map)
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.deny)) { dialog, _ ->
                Toast.makeText(
                    context,
                    resources.getString(R.string.friend_request_deny),
                    Toast.LENGTH_SHORT
                )
                    .show()
                dialog.dismiss()
            }.show()
    }

    private fun inviteFriend() {
        val viewInflated: View = LayoutInflater.from(context)
            .inflate(R.layout.alertdialog_enter_friends_id, view as ViewGroup?, false)
        val enteredId = viewInflated.findViewById<View>(R.id.input_friends_id) as TextInputEditText
        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setView(viewInflated)
            .setTitle(resources.getString(R.string.enter_user_id))
            .setPositiveButton(resources.getString(R.string.add)) { dialog, _ ->
                if (enteredId.text.toString().isNotEmpty()) {
                    if (enteredId.text.toString() != auth.currentUser!!.uid) {
                        //Adding Friend
                        db.collection("Users/${auth.currentUser!!.uid}/FriendList").get()
                            .addOnCompleteListener InviteFriend@{ friendCollection ->
                                if (friendCollection.isSuccessful) {
                                    currentUserFriendList = friendCollection.result.documents
                                    //Getting current user friend list
                                    db.collection("Users")
                                        .get()
                                        .addOnCompleteListener { userCollection ->

                                            //Get all users in Firestore
                                            if (userCollection.isSuccessful) {
                                                val usersList: MutableList<QueryDocumentSnapshot> =
                                                    ArrayList()
                                                for (document in userCollection.result) {
                                                    usersList.add(document)
                                                }

                                                //Searching a friend
                                                for (userDocument in usersList) {
                                                    if (userDocument.getString(MainFragment.KEY_USER_ID) == enteredId.text.toString()) {
                                                        for (friendDocument in currentUserFriendList!!) {
                                                            if (enteredId.text.toString() == friendDocument.getString(
                                                                    MainFragment.KEY_USER_ID
                                                                )
                                                            ) {
                                                                Toast.makeText(
                                                                    context,
                                                                    resources.getString(R.string.error_adding_friend_is_list),
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                return@addOnCompleteListener
                                                            }
                                                        }

                                                        //Get and set a friend info
                                                        val friendMap: MutableMap<String, Any> =
                                                            HashMap()
                                                        friendMap[MainFragment.KEY_USER_ID] =
                                                            userDocument.getString(
                                                                MainFragment.KEY_USER_ID
                                                            )
                                                                .toString()
                                                        friendMap[MainFragment.KEY_USER_REQUEST] =
                                                            0
                                                        friendMap[MainFragment.KEY_USER_PHOTO_URL] =
                                                            userDocument.getString(
                                                                MainFragment.KEY_USER_PHOTO_URL
                                                            )
                                                                .toString()
                                                        friendMap[MainFragment.KEY_USER_NAME] =
                                                            userDocument.getString(
                                                                MainFragment.KEY_USER_NAME
                                                            )
                                                                .toString()
                                                        friendMap[MainFragment.KEY_USER_EMAIL] =
                                                            userDocument.getString(
                                                                MainFragment.KEY_USER_EMAIL
                                                            ).toString()
                                                        friendMap[MainFragment.KEY_USER_FRIEND_ADDING_TIME] =
                                                            System.currentTimeMillis()
                                                        friendMap[MainFragment.KEY_USER_COUNT_COMPLETED_TASKS] =
                                                            userDocument.getLong(
                                                                MainFragment.KEY_USER_COUNT_COMPLETED_TASKS
                                                            )
                                                                ?: 0
                                                        db.document("Users/${auth.currentUser!!.uid}/FriendList/${friendMap[MainFragment.KEY_USER_ID]}")
                                                            .set(friendMap)

                                                        //Send friend request
                                                        val user = Firebase.auth.currentUser
                                                        val userInfo: MutableMap<String, Any> =
                                                            HashMap()
                                                        user?.apply {
                                                            userInfo[MainFragment.KEY_USER_ID] =
                                                                uid
                                                            userInfo[MainFragment.KEY_USER_REQUEST] =
                                                                2
                                                            userInfo[MainFragment.KEY_USER_PHOTO_URL] =
                                                                photoUrl.toString()
                                                            userInfo[MainFragment.KEY_USER_NAME] =
                                                                displayName.toString()
                                                            userInfo[MainFragment.KEY_USER_FRIEND_ADDING_TIME] =
                                                                System.currentTimeMillis()
                                                            userInfo[MainFragment.KEY_USER_EMAIL] =
                                                                email.toString()
                                                            userInfo[MainFragment.KEY_USER_COUNT_COMPLETED_TASKS] =
                                                                totalCompleted
                                                        }
                                                        db.document("Users/${friendMap[MainFragment.KEY_USER_ID]}/FriendList/${auth.currentUser!!.uid}")
                                                            .set(userInfo)
                                                        Toast.makeText(
                                                            context,
                                                            resources.getString(R.string.successfully_adding_friend),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        return@addOnCompleteListener
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            resources.getString(R.string.friend_is_not_exist),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        return@addOnCompleteListener
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                        dialog.dismiss()
                    } else {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.you_add_friend_yourself),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.enter_id_alert),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()

    }

    override fun onFriendClick(view: View, avatar: ImageView, friend: FriendType) {
        if (friend.user_request_code == 1) {
            exitTransition = MaterialElevationScale(false).apply {
                duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            }
            reenterTransition = MaterialElevationScale(true).apply {
                duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            }
            val transitionNameView = resources.getString(R.string.friend_detail_transition_name)
            val extras = FragmentNavigatorExtras(
                view to transitionNameView
            )
            val directions = FriendsFragmentDirections.actionGlobalFriendDetailsFragment(
                friend = friend
            )
            findNavController().navigate(directions, extras)
        } else if (friend.user_request_code == 2) {
            promptRequestFriend(friend.user_id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvFriends.adapter = null
        _binding = null
        hideKeyboard(requireActivity())
    }

}