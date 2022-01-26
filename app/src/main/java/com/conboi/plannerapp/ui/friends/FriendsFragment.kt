package com.conboi.plannerapp.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.*
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.FriendAdapter
import com.conboi.plannerapp.databinding.AlertdialogEnterFriendsIdBinding
import com.conboi.plannerapp.databinding.FragmentFriendsBinding
import com.conboi.plannerapp.model.FriendType
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.main.MainFragment
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.BaseTabFragment
import com.conboi.plannerapp.utils.hideKeyboard
import com.conboi.plannerapp.utils.myclass.NoPreAnGridLayoutManager
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*


@AndroidEntryPoint
@ExperimentalCoroutinesApi
class FriendsFragment : BaseTabFragment(), FriendAdapter.OnFriendListInterface {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

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

    private lateinit var mAdapterFriends: FriendAdapter
    private var adView: AdView? = null

    private var currentUserFriendList: List<DocumentSnapshot>? = null

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
        (requireActivity() as MainActivity).checkPermissions()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).binding.bottomFloatingButton.apply {
            setOnClickListener {
                inviteFriend()
            }
            setOnLongClickListener(null)
        }
        RxJavaPlugins.setErrorHandler { }
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

        //Friends RV
        mAdapterFriends = FriendAdapter(options, this)
        binding.rvFriends.apply {
            adapter = mAdapterFriends
            layoutManager =
                NoPreAnGridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
        }
        lifecycleScope.launch {
            mAdapterFriends.loadStateFlow.collectLatest { loadStates ->
                _binding?.apply {
                    when (loadStates.refresh) {
                        is LoadState.Error -> {
                            tvNoFriends.visibility = View.VISIBLE
                            swiperefresh.isRefreshing = false
                            Toast.makeText(
                                context,
                                resources.getString(R.string.error_loading_friends),
                                Toast.LENGTH_SHORT
                            ).show()
                            // The initial load failed. Call the retry() method
                            // in order to retry the load operation.
                            // ...
                        }
                        is LoadState.NotLoading -> {
                            swiperefresh.isRefreshing = false
                        }


                        else -> {}
                    }
                    when (loadStates.append) {
                        is LoadState.Error -> {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.there_are_no_more_friends_for_loading),
                                Toast.LENGTH_SHORT
                            ).show()
                            // The additional load failed. Call the retry() method
                            // in order to retry the load operation.
                            // ...
                        }
                        is LoadState.NotLoading -> {
                            if (loadStates.refresh is LoadState.NotLoading) {
                                swiperefresh.isRefreshing = false
                                // The previous load (either initial or additional) completed
                                // ...
                            }
                        }
                        else -> {}
                    }
                }
            }

        }

        checkFriendsList()

        sharedViewModel.premiumState.observe(this.viewLifecycleOwner) {
            if (it) {
                binding.adView.visibility = View.GONE
                binding.swiperefresh.updatePadding(top = 0)
            } else {
                binding.adView.visibility = View.VISIBLE
                val scale = resources.displayMetrics.density
                binding.swiperefresh.updatePadding(top = (50 * scale + 0.5f).toInt())
                adView = binding.adView
                adView?.loadAd(AdRequest.Builder().build())
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            mAdapterFriends.refresh()
            checkFriendsList()
        }

    }


    private fun checkFriendsList() {
        lifecycleScope.launchWhenCreated {
            db.collection("Users/${auth.currentUser!!.uid}/FriendList")
                .get()
                .addOnSuccessListener { friendsCollection ->
                    if (!friendsCollection.isEmpty) {
                        currentUserFriendList = friendsCollection.documents
                        checkEveryFriend()
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

    private fun checkEveryFriend() {
        lifecycleScope.launchWhenCreated {
            //Getting friend list
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
                            binding.tvNoFriends.visibility = View.VISIBLE
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
                                    friendMap[MainFragment.KEY_USER_PRIVATE_MODE] =
                                        userDocument.getBoolean(MainFragment.KEY_USER_PRIVATE_MODE)
                                            ?: false

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
                mAdapterFriends.refresh()
                checkFriendsList()
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(R.string.deny)) { dialog, _ ->
                val map: MutableMap<String, Any> = HashMap()
                map[MainFragment.KEY_USER_REQUEST] = 3
                db.document("Users/${friendId}/FriendList/${auth.currentUser!!.uid}")
                    .update(map)
                db.document("Users/${auth.currentUser!!.uid}/FriendList/${friendId}")
                    .delete()
                Toast.makeText(
                    context,
                    resources.getString(R.string.friend_request_deny),
                    Toast.LENGTH_SHORT
                )
                    .show()
                mAdapterFriends.refresh()
                dialog.dismiss()
            }.show()
    }

    private fun inviteFriend() {
        val totalCompleted = sharedViewModel.totalCompleted.value
        val friendIdBinding: AlertdialogEnterFriendsIdBinding =
            AlertdialogEnterFriendsIdBinding.inflate(layoutInflater)

        friendIdBinding.inputId.addTextChangedListener {
            if (it?.toString()?.isNotBlank() == true) {
                friendIdBinding.inputIdLayout.error = null
            }
        }

        val enterIdDialog = MaterialAlertDialogBuilder(
            requireContext()
        )
            .setView(friendIdBinding.root)
            .setTitle(resources.getString(R.string.send_friend))
            .setPositiveButton(resources.getString(R.string.add), null)
            .setNegativeButton(resources.getString(R.string.cancel), null)
            .create()

        enterIdDialog.setOnShowListener { dialog ->
            val positiveButton = (dialog as AlertDialog).getButton(
                AlertDialog.BUTTON_POSITIVE
            )
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setOnClickListener {
                val enteredId = friendIdBinding.inputId.text.toString().trim()
                if (enteredId.isNotBlank()) {
                    if (enteredId != auth.currentUser!!.uid) {
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
                                                    if (userDocument.getString(MainFragment.KEY_USER_ID) == enteredId) {

                                                        for (friendDocument in currentUserFriendList!!) {
                                                            if (enteredId == friendDocument.getString(
                                                                    MainFragment.KEY_USER_ID
                                                                )
                                                            ) {
                                                                friendIdBinding.inputIdLayout.error =
                                                                    resources.getString(R.string.error_adding_friend_is_list)
                                                                friendIdBinding.inputId.text = null
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
                                                        friendMap[MainFragment.KEY_USER_PRIVATE_MODE] =
                                                            userDocument.getBoolean(MainFragment.KEY_USER_PRIVATE_MODE)
                                                                ?: false

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
                                                                totalCompleted ?: 0
                                                            userInfo[MainFragment.KEY_USER_PRIVATE_MODE] =
                                                                sharedViewModel.privateModeState.value
                                                                    ?: false

                                                        }
                                                        db.document("Users/${friendMap[MainFragment.KEY_USER_ID]}/FriendList/${auth.currentUser!!.uid}")
                                                            .set(userInfo)
                                                        Toast.makeText(
                                                            context,
                                                            resources.getString(R.string.successfully_adding_friend),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        mAdapterFriends.refresh()
                                                        checkFriendsList()
                                                        dialog.dismiss()
                                                        return@addOnCompleteListener
                                                    } else {
                                                        friendIdBinding.inputIdLayout.error =
                                                            resources.getString(R.string.friend_is_not_exist)
                                                        friendIdBinding.inputId.text = null
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                    } else {
                        friendIdBinding.inputIdLayout.error =
                            resources.getString(R.string.you_add_friend_yourself)
                        friendIdBinding.inputId.text = null
                    }
                } else {
                    friendIdBinding.inputIdLayout.error =
                        resources.getString(R.string.enter_id_alert)
                }
            }
            negativeButton.setOnClickListener {
                dialog.cancel()
            }
        }
        enterIdDialog.show()
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
            val directions = FriendsFragmentDirections.actionFriendsFragmentToFriendDetailsFragment(
                friend = friend
            )
            findNavController().navigate(directions, extras)
        } else if (friend.user_request_code == 2) {
            promptRequestFriend(friend.user_id)
        }
    }

    override fun onFriendHold(view: View, friendId: String, friendName: String) {
        PopupMenu(requireContext(), view).apply {
            menuInflater.inflate(R.menu.popup_menu_friend_options, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_delete_friend -> {
                        db.collection("Users/${auth.currentUser!!.uid}/FriendList")
                            .document(friendId).delete()
                        db.collection("Users/$friendId/FriendList")
                            .document(auth.currentUser!!.uid).delete()
                        mAdapterFriends.refresh()
                        checkFriendsList()
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.friend_removed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                true
            }
            show()
        }

    }


    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
        mAdapterFriends.refresh()
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvFriends.adapter = null
        _binding = null
        adView = null
        hideKeyboard(requireActivity())
    }

}