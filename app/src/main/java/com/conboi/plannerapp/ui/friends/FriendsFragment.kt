package com.conboi.plannerapp.ui.friends

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.FriendAdapter
import com.conboi.plannerapp.data.model.FriendType
import com.conboi.plannerapp.databinding.FragmentFriendsBinding
import com.conboi.plannerapp.ui.main.MainFragment
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.BaseTabFragment
import com.conboi.plannerapp.utils.hideKeyboard
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class FriendsFragment : BaseTabFragment(), FriendAdapter.OnFriendListInterface {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by viewModels()
    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    //Adapter for friend list
    private val mAdapterFriends: FriendAdapter = FriendAdapter(this)

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
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialFriendList()
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        //Material Animation
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        exitTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        requireActivity().bottom_floating_button.apply {
            setOnClickListener {
                inviteFriend()
            }
            setOnLongClickListener(null)
        }

        view.background.alpha = 35

        //Friends RV
        binding.rvFriends.apply {
            adapter = mAdapterFriends
            layoutManager = GridLayoutManager(context, 2, GridLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.UP
            ) {
                val vb =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requireActivity().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as Vibrator?
                    } else {
                        requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
                    }

                override fun onSelectedChanged(
                    viewHolder: RecyclerView.ViewHolder?,
                    actionState: Int
                ) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                        vb!!.vibrate(
                            VibrationEffect.createOneShot(
                                50,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    }
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    vb!!.vibrate(
                        VibrationEffect.createOneShot(
                            25,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                    /*TODO("Delete")
                    mAdapterFriends.deleteFriend(viewHolder.bindingAdapterPosition)*/
                    db.document(
                        "Users/" +
                                mAdapterFriends.currentList[viewHolder.bindingAdapterPosition].user_id +
                                "/FriendList/${auth.currentUser!!.uid}"
                    )
                        .delete()
                    Toast.makeText(
                        context,
                        "${mAdapterFriends.currentList[viewHolder.bindingAdapterPosition].user_name} has been deleted",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }).attachToRecyclerView(this)
        }

        val helper: SnapHelper = LinearSnapHelper()
        helper.attachToRecyclerView(binding.rvFriends)
    }


    override fun onFriendClick(view: View, friend: FriendType) {
        if (friend.user_request_code == 1) {
            exitTransition = MaterialElevationScale(false).apply {
                duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            }
            reenterTransition = MaterialElevationScale(false).apply {
                duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            }
            val transitionName = resources.getString(R.string.friend_detail_transition_name)
            val extras = FragmentNavigatorExtras(view to transitionName)
            val directions = FriendsFragmentDirections.actionGlobalFriendDetailsFragment(
                friend = friend
            )
            findNavController().navigate(directions, extras)
        } else if (friend.user_request_code == 2) {
            addingFriend(friend.user_id)
        }
    }

    private fun addingFriend(friend_id: String) {
        //Adding new friend
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Do you want to add this user to friends?")
            .setPositiveButton("Confirm") { dialog, _ ->
                val map: MutableMap<String, Any> = HashMap()
                map[MainFragment.KEY_USER_REQUEST] = 1
                db.document("Users/${friend_id}/FriendList/${auth.currentUser!!.uid}")
                    .update(map)
                db.document("Users/${auth.currentUser!!.uid}/FriendList/${friend_id}")
                    .update(map)
                dialog.dismiss()
            }
            .setNegativeButton("Deny") { dialog, _ ->
                Toast.makeText(context, "User request has been denied", Toast.LENGTH_SHORT)
                    .show()
                dialog.dismiss()
            }.show()
    }

    private fun inviteFriend() {
        val viewInflated: View = LayoutInflater.from(requireContext())
            .inflate(R.layout.alertdialog_enter_friends_id, view as ViewGroup?, false)
        val enteredId = viewInflated.findViewById<View>(R.id.input_friends_id) as EditText
        MaterialAlertDialogBuilder(
            requireContext(),
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
        )
            .setView(viewInflated)
            .setMessage(resources.getString(R.string.enter_friends_id))
            .setPositiveButton(resources.getString(R.string.add)) { dialog, _ ->
                if (enteredId.text.toString().isNotEmpty()) {
                    if (enteredId.text.toString() != auth.currentUser!!.uid) {
                        //Adding Friend
                        db.collection("Users/${auth.currentUser!!.uid}/FriendList").get()
                            .addOnCompleteListener { friendsCollection ->
                                if (friendsCollection.isSuccessful) {
                                    var totalCompleted = 0
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        totalCompleted =
                                            viewModel.preferencesFlow.first().totalCompleted
                                    }
                                    //Getting current user friend list
                                    db.collection("Users")
                                        .get()
                                        .addOnCompleteListener { userCollection ->

                                            //Get all users in Firestore
                                            if (userCollection.isSuccessful) {
                                                val listDocuments: MutableList<QueryDocumentSnapshot> =
                                                    ArrayList()
                                                for (document in userCollection.result) {
                                                    listDocuments.add(document)
                                                }

                                                //Searching a friend
                                                for (i in 0 until listDocuments.size) {
                                                    if (listDocuments[i].getString(MainFragment.KEY_USER_ID) == enteredId.text.toString()) {
                                                        for (element in currentUserFriendList!!) {
                                                            if (enteredId.text.toString() == element.getString(
                                                                    MainFragment.KEY_USER_ID
                                                                )
                                                            ) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Error adding the friend, is already in your list",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                break
                                                            }
                                                        }

                                                        //Get and set a friend info
                                                        val friendMap: MutableMap<String, Any> =
                                                            HashMap()
                                                        friendMap[MainFragment.KEY_USER_ID] =
                                                            listDocuments[i].getString(
                                                                MainFragment.KEY_USER_ID
                                                            )
                                                                .toString()
                                                        friendMap[MainFragment.KEY_USER_REQUEST] =
                                                            0
                                                        friendMap[MainFragment.KEY_USER_PHOTO_URL] =
                                                            listDocuments[i].getString(
                                                                MainFragment.KEY_USER_PHOTO_URL
                                                            )
                                                                .toString()
                                                        friendMap[MainFragment.KEY_USER_NAME] =
                                                            listDocuments[i].getString(
                                                                MainFragment.KEY_USER_NAME
                                                            )
                                                                .toString()
                                                        friendMap[MainFragment.KEY_USER_EMAIL] =
                                                            listDocuments[i].getString(
                                                                MainFragment.KEY_USER_EMAIL
                                                            ).toString()
                                                        friendMap[MainFragment.KEY_USER_COUNT_COMPLETED_TASKS] =
                                                            listDocuments[i].getLong(
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
                                                            userInfo[MainFragment.KEY_USER_EMAIL] =
                                                                email.toString()
                                                            userInfo[MainFragment.KEY_USER_COUNT_COMPLETED_TASKS] =
                                                                totalCompleted
                                                        }
                                                        db.document("Users/${friendMap[MainFragment.KEY_USER_ID]}/FriendList/${auth.currentUser!!.uid}")
                                                            .set(userInfo)
                                                        Toast.makeText(
                                                            context,
                                                            "Successfully added new friend",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        break
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "The Friend you are looking for doesn't exist",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
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
                            "You can't add yourself to your friend list",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Enter nickname!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()

    }


    private fun initialFriendList() {
        db.collection("Users/${auth.currentUser!!.uid}/FriendList").get()
            .addOnCompleteListener { friendsCollection ->
                if (friendsCollection.isSuccessful) {
                    if (friendsCollection.result.documents.isNotEmpty()) {
                        //Getting current user friend list
                        currentUserFriendList = friendsCollection.result.documents
                        val listDocuments: MutableList<QueryDocumentSnapshot> =
                            ArrayList()
                        for (document in friendsCollection.result) {
                            listDocuments.add(document)
                        }
                        val listOfFriends: MutableList<FriendType> = ArrayList()
                        for (i in 0 until listDocuments.size) {
                            //Get and set a friend info
                            val gotFriend = FriendType(
                                user_id = listDocuments[i].getString(
                                    MainFragment.KEY_USER_ID
                                )
                                    .toString(),
                                user_name = listDocuments[i].getString(
                                    MainFragment.KEY_USER_NAME
                                )
                                    .toString(),
                                user_email = listDocuments[i].getString(
                                    MainFragment.KEY_USER_EMAIL
                                ).toString(),
                                user_photo_url = listDocuments[i].getString(
                                    MainFragment.KEY_USER_PHOTO_URL
                                )
                                    .toString(),
                                user_request_code = listDocuments[i].getLong(
                                    MainFragment.KEY_USER_REQUEST
                                )!!.toInt(),
                                user_total_completed = listDocuments[i].getLong(
                                    MainFragment.KEY_USER_COUNT_COMPLETED_TASKS
                                )?.toInt() ?: 0
                            )
                            listOfFriends.add(gotFriend)
                        }
                        mAdapterFriends.submitList(listOfFriends)
                    } else {
                        //TODO("There are no friends")
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentUserFriendList = null
        _binding = null
        hideKeyboard(requireActivity())
    }

}