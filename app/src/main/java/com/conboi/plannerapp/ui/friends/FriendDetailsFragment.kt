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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.FriendTasksAdapter
import com.conboi.plannerapp.databinding.FragmentFriendDetailsBinding
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.themeColor
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.transition.MaterialContainerTransform
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.recyclerview.animators.FadeInAnimator
import kotlin.math.abs


@AndroidEntryPoint
class FriendDetailsFragment : Fragment() {
    private var _binding: FragmentFriendDetailsBinding? = null
    val binding get() = _binding!!

    private val navigationArgs: FriendDetailsFragmentArgs by navArgs()

    private val friendDetailsViewModel: FriendDetailsViewModel by viewModels()

    private val db = FirebaseFirestore.getInstance()

    private val mAdapterFriendTask = FriendTasksAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.navigation_host
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().themeColor(com.google.android.material.R.attr.colorSurface))
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
        with(binding) {
            lifecycleOwner = this@FriendDetailsFragment
            toolbar.setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            appBarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout, verticalOffset ->
                if (abs(verticalOffset) == appBarLayout.totalScrollRange) {
                    TransitionManager.beginDelayedTransition(
                        binding.parentFriendDetails,
                        Fade()
                    )
                    binding.friendAvatar.visibility = View.INVISIBLE
                } else if (verticalOffset <= -0.5) {
                    TransitionManager.beginDelayedTransition(
                        binding.parentFriendDetails,
                        Fade()
                    )
                    binding.friendAvatar.visibility = View.VISIBLE
                }
            })
            rvFriendTask.apply {
                adapter = mAdapterFriendTask
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                itemAnimator = FadeInAnimator().apply {
                    changeDuration = 300
                    addDuration = 100
                    removeDuration = 100
                }
            }
            navigationArgs.apply {
                binding.friend = friend
                if (friend != friendDetailsViewModel.bufferFriend.value) {
                    friendDetailsViewModel.setBufferFriend(friend)
                }
                getFriendTasks(friend.user_id, friend.user_name, friend.user_private_mode)
            }

        }
    }

    private fun getFriendTasks(friendId: String, friendName: String, private_mode: Boolean) {
        binding.circularLoading.visibility = View.VISIBLE
        if (!private_mode) {
            //Getting friend's tasks
            db.document("Users/${friendId}/TaskList/Tasks").get()
                .addOnCompleteListener { taskCollection ->
                    if (taskCollection.isSuccessful) {
                        val currentTasksList: MutableList<String> = ArrayList()
                        val taskDocument = taskCollection.result
                        val mapFromTasks: MutableMap<String, Any>? = taskCollection.result.data
                        if (mapFromTasks != null) {
                            val lateinitTaskList: MutableList<TaskType> = ArrayList()
                            //Get map value from key task
                            for ((key) in mapFromTasks) {
                                currentTasksList.add(key)
                            }

                            for (task in currentTasksList) {
                                val idTask = currentTasksList.size + currentTasksList.indexOf(task)
                                if (taskDocument.getString("$task.${TaskType.TaskEntry.COLUMN_TITLE}")
                                        .toString().isNotBlank()
                                ) {
                                    val taskType = TaskType(
                                        idTask = idTask,
                                        title = taskDocument.getString("$task.${TaskType.TaskEntry.COLUMN_TITLE}")
                                            ?: "Error getting title",
                                        priority = taskDocument.getLong("$task.${TaskType.TaskEntry.COLUMN_PRIORITY}")
                                            ?.toInt() ?: 1,
                                        totalChecked = taskDocument.getLong("$task.${TaskType.TaskEntry.COLUMN_TOTAL_CHECKED}")
                                            ?.toInt() ?: 0,
                                        checked = taskDocument.getBoolean("$task.${TaskType.TaskEntry.COLUMN_CHECKED}")
                                            ?: false,
                                        completed = taskDocument.getLong("$task.${TaskType.TaskEntry.COLUMN_COMPLETED}")
                                            ?: GLOBAL_START_DATE
                                    )
                                    lateinitTaskList.add(taskType)
                                }
                            }
                            val sortedList =
                                lateinitTaskList.sortedBy { it.title }
                                    .sortedByDescending { it.priority }
                            val filter = lateinitTaskList.filter { it.checked }
                            friendDetailsViewModel.setFriendsTasksList(sortedList)
                            mAdapterFriendTask.submitList(sortedList)
                            binding.circularLoading.visibility = View.GONE
                            binding.countOfTasks.text = resources.getString(
                                R.string.count_of_tasks,
                                filter.size,
                                sortedList.size
                            )
                        } else {
                            binding.circularLoading.visibility = View.GONE
                            binding.tvTasksMsg.text = resources.getString(
                                R.string.there_will_be_shown_friends_tasks,
                                friendName
                            )
                            binding.tvTasksMsg.visibility = View.VISIBLE
                            Toast.makeText(
                                context,
                                resources.getString(R.string.friends_tasks_empty),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        binding.circularLoading.visibility = View.GONE
                        binding.tvTasksMsg.text = resources.getString(
                            R.string.there_will_be_shown_friends_tasks,
                            friendName
                        )
                        binding.tvTasksMsg.visibility = View.VISIBLE
                        Toast.makeText(
                            context,
                            resources.getString(R.string.friends_tasks_empty),
                            Toast.LENGTH_SHORT
                        ).show()
                        mAdapterFriendTask.submitList(friendDetailsViewModel.friendsTasks.value)
                    }
                }
        } else {
            binding.circularLoading.visibility = View.GONE
            binding.tvTasksMsg.text =
                resources.getString(R.string.friends_tasks_private, friendName)
            binding.tvTasksMsg.visibility = View.VISIBLE
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        friendDetailsViewModel.saveState()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        friendDetailsViewModel.retrieveState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
