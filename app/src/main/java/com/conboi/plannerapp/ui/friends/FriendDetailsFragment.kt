package com.conboi.plannerapp.ui.friends


import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.FriendTasksAdapter
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.databinding.FragmentFriendDetailsBinding
import com.conboi.plannerapp.utils.themeColor
import com.google.android.material.transition.MaterialContainerTransform
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FriendDetailsFragment : Fragment() {
    private var _binding: FragmentFriendDetailsBinding? = null
    val binding get() = _binding!!

    private val navigationArgs: FriendDetailsFragmentArgs by navArgs()

    private val db = FirebaseFirestore.getInstance()

    private val taskList: MutableList<TaskType> = ArrayList()
    private val mAdapterFriendTask = FriendTasksAdapter(taskList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.navigation_host
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().themeColor(R.attr.colorSurface))
        }
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            this.isEnabled = true
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_friend_details, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            lifecycleOwner = this@FriendDetailsFragment
            fragmentFriendDetailsToolbar.setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            binding.friend = navigationArgs.friend
            getFriendTasks(navigationArgs.friend.user_id)
            

            rvFriendTask.apply {
                adapter = mAdapterFriendTask
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
            }
        }
    }


    private fun getFriendTasks(friend_id: String) {
        //Getting friend's tasks
        db.document("Users/${friend_id}/TaskList/Tasks").get()
            .addOnCompleteListener { taskCollection ->
                if (taskCollection.isSuccessful) {
                    val tasks: MutableList<String> = ArrayList()
                    val map: MutableMap<String, Any>? = taskCollection.result.data
                    val document = taskCollection.result
                    if (map != null) {
                        for ((key) in map) {
                            tasks.add(key)
                        }

                        for (i in 0 until tasks.size) {
                            val idTask = tasks.size + i
                            val taskType = TaskType(
                                idTask = idTask,
                                title = document.getString("${tasks[i]}.${TaskType.TaskEntry.COLUMN_TITLE}")!!,
                                description = document.getString("${tasks[i]}.${TaskType.TaskEntry.COLUMN_DESCRIPTION}")!!,
                                time = document.getLong("${tasks[i]}.${TaskType.TaskEntry.COLUMN_TIME}")!!
                                    .toInt(),
                                priority = document.getLong("${tasks[i]}.${TaskType.TaskEntry.COLUMN_PRIORITY}")!!
                                    .toInt(),
                                checked = document.getBoolean("${tasks[i]}.${TaskType.TaskEntry.COLUMN_CHECKED}")!!,
                                created = document.getLong("${tasks[i]}.${TaskType.TaskEntry.COLUMN_CREATED}")!!,
                                completed = document.getLong("${tasks[i]}.${TaskType.TaskEntry.COLUMN_COMPLETED}")!!
                            )
                            taskList.add(taskType)
                        }
                        mAdapterFriendTask.submitList(taskList)
                    } else {
                        Toast.makeText(
                            context,
                            "Friend taskCollection list is empty",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
