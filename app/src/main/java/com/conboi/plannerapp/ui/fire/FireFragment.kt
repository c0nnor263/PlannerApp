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
import com.conboi.plannerapp.data.FriendType
import com.conboi.plannerapp.databinding.FragmentFireBinding
import com.conboi.plannerapp.databinding.ListFriendBinding
import com.conboi.plannerapp.utils.hideKeyboard
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

const val KEY_FRIEND_ID = "idFriend"

@AndroidEntryPoint
class FireFragment : Fragment(R.layout.fragment_fire), FriendAdapter.OnFriendListInterface {
    private var _binding: FragmentFireBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var mAdapter: FriendAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val viewModel: FireViewModel by viewModels()
    private var bufferFriendList: List<DocumentSnapshot>? = null


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


        db.collection("Users/${auth.currentUser!!.uid}/FriendList").get()
            .addOnSuccessListener { collection ->
                if (collection.isEmpty) {
                    val userFriendList: MutableMap<String, Any> = HashMap()
                    userFriendList["idFriend"] = "Add"
                    db.document("Users/${auth.currentUser!!.uid}/FriendList/!!!Add")
                        .set(userFriendList)
                }
            }

        val query: Query = db.collection("Users/${auth.currentUser!!.uid}/FriendList")
        val options = FirestoreRecyclerOptions.Builder<FriendType>()
            .setQuery(query, FriendType::class.java)
            .build()

        mAdapter = FriendAdapter(options, this)
        binding.apply {
            recyclerView = recyclerViewFriends
        }
        recyclerView.apply {
            adapter = mAdapter
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
                    mAdapter.deleteFriend(viewHolder.bindingAdapterPosition)
                    Toast.makeText(
                        context,
                        "${mAdapter.getItem(viewHolder.bindingAdapterPosition).idFriend} has been deleted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }).attachToRecyclerView(this)
        }

        val helper: SnapHelper = LinearSnapHelper()
        helper.attachToRecyclerView(recyclerView)
    }

    override fun onFriendClick(position: Int) {
        val friend = mAdapter.getItem(position)
        if (friend.idFriend == "Add") {
            val alertDialog = MaterialAlertDialogBuilder(requireContext())
            val input = EditText(context)
            input.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            input.requestFocus()
            alertDialog.setTitle("Enter friend's nickname")
            alertDialog.setView(input)

            alertDialog.setPositiveButton("Add") { dialog, _ ->
                if (input.text.toString().isNotEmpty()) {
                    if (input.text.toString() != auth.currentUser!!.uid) {
                        db.collection("Users/${auth.currentUser!!.uid}/FriendList").get()
                            .addOnCompleteListener { taskCollection ->
                                if (taskCollection.isSuccessful) {
                                    bufferFriendList = taskCollection.result.documents
                                }
                            }
                        db.collection("Users")
                            .get()
                            .addOnCompleteListener { taskCollection ->
                                if (taskCollection.isSuccessful) {
                                    val listDocuments: MutableList<QueryDocumentSnapshot> =
                                        ArrayList()
                                    for (document in taskCollection.result) {
                                        listDocuments.add(document)
                                    }

                                    for (i in 0 until listDocuments.size) {
                                        if (listDocuments[i].getString("user_id") == input.text.toString()) {
                                            for (element in bufferFriendList!!) {
                                                if (input.text.toString() == element.getString(
                                                        "idFriend"
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

                                            val friendMap: MutableMap<String, Any> = HashMap()
                                            friendMap[KEY_FRIEND_ID] = input.text.toString()
                                            db.document("Users/${auth.currentUser!!.uid}/FriendList/${friendMap[KEY_FRIEND_ID]}")
                                                .set(friendMap)

                                            recyclerView.smoothScrollToPosition(bufferFriendList!!.lastIndex)
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
        }

    }



    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        mAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        mAdapter.stopListening()
        mAdapter.notifyDataSetChanged()
    }


}