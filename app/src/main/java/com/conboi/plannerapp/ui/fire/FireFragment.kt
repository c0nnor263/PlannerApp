package com.conboi.plannerapp.ui.fire

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.*
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.FriendAdapter
import com.conboi.plannerapp.adapter.FriendTasksAdapter
import com.conboi.plannerapp.data.FriendType
import com.conboi.plannerapp.data.TaskType
import com.conboi.plannerapp.databinding.FragmentFireBinding
import com.conboi.plannerapp.ui.water.*
import com.conboi.plannerapp.utils.hideKeyboard
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class FireFragment : Fragment(R.layout.fragment_fire), FriendAdapter.OnFriendListInterface {
    private var _binding: FragmentFireBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerViewFriends: RecyclerView
    private lateinit var mAdapterFriends: FriendAdapter

    private lateinit var recyclerViewFriendTask: RecyclerView
    private lateinit var mAdapterFriendTask: FriendTasksAdapter
    private val tasksList: MutableList<TaskType> = ArrayList()


    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val viewModel: FireViewModel by viewModels()
    private var currentUserFriendList: List<DocumentSnapshot>? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        //Initial set of FriendList
        db.collection("Users/${auth.currentUser!!.uid}/FriendList").get()
            .addOnSuccessListener { collection ->
                if (collection.isEmpty) {
                    val userFriendList: MutableMap<String, Any> = HashMap()
                    userFriendList[WaterFragment.KEY_USER_ID] = "Add"
                    db.document("Users/${auth.currentUser!!.uid}/FriendList/!!!Add")
                        .set(userFriendList)
                }
            }
        val query: Query = db.collection("Users/${auth.currentUser!!.uid}/FriendList")
        val options = FirestoreRecyclerOptions.Builder<FriendType>()
            .setQuery(query, FriendType::class.java)
            .build()


        binding.apply {
            recyclerViewFriends = rvFriends
            recyclerViewFriendTask = rvFriendTask
        }

        mAdapterFriends = FriendAdapter(options, this)
        mAdapterFriendTask = FriendTasksAdapter(tasksList)

        recyclerViewFriends.apply {
            adapter = mAdapterFriends
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            smoothScrollToPosition(2)
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
                    mAdapterFriends.deleteFriend(viewHolder.bindingAdapterPosition)
                    val map:MutableMap<String,Any> = HashMap()
                    map[WaterFragment.KEY_USER_REQUEST] = 0
                    db.document("Users/${mAdapterFriends.getItem(viewHolder.bindingAdapterPosition).user_id}/FriendList/${auth.currentUser!!.uid}")
                        .update(map)
                    Toast.makeText(
                        context,
                        "${mAdapterFriends.getItem(viewHolder.bindingAdapterPosition).user_id} has been deleted",
                        Toast.LENGTH_SHORT
                    ).show()

                    mAdapterFriendTask.submitList(ArrayList())
                }
            }).attachToRecyclerView(this)
        }
        recyclerViewFriendTask.apply {
            adapter = mAdapterFriendTask
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        val helper: SnapHelper = LinearSnapHelper()
        helper.attachToRecyclerView(recyclerViewFriends)
    }

    override fun onFriendClick(position: Int) {
        val friend = mAdapterFriends.getItem(position)
        if (friend.user_id == "Add") {
            val alertDialog = MaterialAlertDialogBuilder(requireContext())
            val input = EditText(context)
            input.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            input.requestFocus()
            alertDialog.setTitle("Enter friend's email")
            alertDialog.setView(input)

            alertDialog.setPositiveButton("Add") { dialog, _ ->
                if (input.text.toString().isNotEmpty()) {
                    if (input.text.toString() != auth.currentUser!!.uid) {
                        db.collection("Users/${auth.currentUser!!.uid}/FriendList").get()
                            .addOnCompleteListener { taskCollection ->
                                if (taskCollection.isSuccessful) {
                                    currentUserFriendList = taskCollection.result.documents
                                }
                            }
                        db.collection("Users")
                            .get()
                            .addOnCompleteListener { taskCollection ->
                                //Get all users in Firestore
                                if (taskCollection.isSuccessful) {
                                    val listDocuments: MutableList<QueryDocumentSnapshot> =
                                        ArrayList()
                                    for (document in taskCollection.result) {
                                        listDocuments.add(document)
                                    }

                                    //Searching a friend
                                    for (i in 0 until listDocuments.size) {
                                        if (listDocuments[i].getString(WaterFragment.KEY_USER_EMAIL) == input.text.toString()) {
                                            for (element in currentUserFriendList!!) {
                                                if (input.text.toString() == element.getString(
                                                        WaterFragment.KEY_USER_EMAIL
                                                    )
                                                ) {
                                                    Toast.makeText(
                                                        context,
                                                        "Error adding the friend, is already in your list",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    return@addOnCompleteListener
                                                }
                                            }
                                            Toast.makeText(
                                                context,
                                                "Successfully added new friend",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            //Get and set a friend info
                                            val friendMap: MutableMap<String, Any> = HashMap()
                                            friendMap[WaterFragment.KEY_USER_ID] =
                                                listDocuments[i].getString(WaterFragment.KEY_USER_ID)
                                                    .toString()
                                            friendMap[WaterFragment.KEY_USER_REQUEST] = 0
                                            friendMap[WaterFragment.KEY_USER_PHOTO_URL] =
                                                listDocuments[i].getString(WaterFragment.KEY_USER_PHOTO_URL)
                                                    .toString()
                                            friendMap[WaterFragment.KEY_USER_NAME] =
                                                listDocuments[i].getString(WaterFragment.KEY_USER_NAME)
                                                    .toString()
                                            friendMap[WaterFragment.KEY_USER_EMAIL] =
                                                input.text.toString()

                                            db.document("Users/${auth.currentUser!!.uid}/FriendList/${friendMap[WaterFragment.KEY_USER_ID]}")
                                                .set(friendMap)

                                            //Send friend request
                                            val user = Firebase.auth.currentUser
                                            val userInfo: MutableMap<String, Any> = HashMap()
                                            user?.let {
                                                userInfo[WaterFragment.KEY_USER_ID] = user.uid
                                                userInfo[WaterFragment.KEY_USER_REQUEST] = 2
                                                userInfo[WaterFragment.KEY_USER_PHOTO_URL] =
                                                    user.photoUrl.toString()
                                                userInfo[WaterFragment.KEY_USER_NAME] =
                                                    user.displayName.toString()
                                                userInfo[WaterFragment.KEY_USER_EMAIL] =
                                                    user.email.toString()
                                            }
                                            db.document("Users/${friendMap[WaterFragment.KEY_USER_ID]}/FriendList/${auth.currentUser!!.uid}")
                                                .set(userInfo)

                                            recyclerViewFriends.smoothScrollToPosition(
                                                currentUserFriendList!!.lastIndex
                                            )
                                            return@addOnCompleteListener
                                        }

                                    }
                                    Toast.makeText(
                                        context,
                                        "The Friend you are looking for doesn't exist",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
            alertDialog.show()
        } else {
            if (friend.user_request_code == 1) {
                db.document("Users/${friend.user_id}/TaskList/Tasks").get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val list: MutableList<String> = ArrayList()
                            val map: MutableMap<String, Any>? = task.result.data
                            val document = task.result
                            if (map != null) {
                                for ((key) in map) {
                                    list.add(key)
                                }

                                for (i in 0 until list.size) {
                                    val idTask = list.size + i
                                    val taskType = TaskType(
                                        idTask = idTask,
                                        nameTask = document.getString("${list[i]}.nameTask")!!,
                                        descriptionTask = document.getString("${list[i]}.descriptionTask")!!,
                                        timeTask = document.getLong("${list[i]}.timeTask")!!
                                            .toInt(),
                                        priorityTask = document.getLong("${list[i]}.priorityTask")!!
                                            .toInt(),
                                        checkTask = document.getBoolean("${list[i]}.checkTask")!!,
                                    )
                                    tasksList.add(taskType)
                                }
                                mAdapterFriendTask.submitList(tasksList)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Friend task list is empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
            } else if (friend.user_request_code == 2) {
                val alertDialog = MaterialAlertDialogBuilder(requireContext())
                alertDialog.setTitle("Do you want to add this user to friends?")
                alertDialog.setPositiveButton("Confirm") { dialog, _ ->
                    val map: MutableMap<String, Any> = HashMap()
                    map[WaterFragment.KEY_USER_REQUEST] = 1
                    db.document("Users/${friend.user_id}/FriendList/${auth.currentUser!!.uid}")
                        .update(map)
                    db.document("Users/${auth.currentUser!!.uid}/FriendList/${friend.user_id}")
                        .update(map)
                    dialog.dismiss()
                }
                alertDialog.setNegativeButton("Deny") { dialog, _ ->
                    Toast.makeText(context, "User request has been denied", Toast.LENGTH_SHORT)
                        .show()
                    dialog.dismiss()
                }.show()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        mAdapterFriends.startListening()
    }

    override fun onStop() {
        super.onStop()
        mAdapterFriends.stopListening()
        mAdapterFriends.notifyDataSetChanged()
    }


}