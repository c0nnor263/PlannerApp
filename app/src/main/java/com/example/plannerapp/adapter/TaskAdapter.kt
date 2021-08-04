package com.example.plannerapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.ViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.plannerapp.data.TaskType
import com.example.plannerapp.databinding.ListTaskBinding
import com.example.plannerapp.ui.water.WaterFragmentDirections
import com.google.android.material.textfield.TextInputLayout
import java.util.*
import kotlin.concurrent.schedule


class TaskAdapter(
    private val listener: OnItemClickListener
) : ListAdapter<TaskType, TaskAdapter.ViewHolder>(DiffCallback) {
    inner class ViewHolder(private var binding: ListTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                openTask.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = getItem(position)
                        val transitionName =
                            root.context.getString(com.example.plannerapp.R.string.edit_task_transition)
                        val extras = FragmentNavigatorExtras(root to transitionName)
                        val directions = WaterFragmentDirections.actionWaterFragmentToEditTask(
                            idTask = task.idTask
                        )
                        root.findNavController().navigate(directions, extras)
                    }
                }
                checkTask.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = getItem(position)
                        listener.onCheckBoxClick(task, checkTask.isChecked)
                    }
                }
                nameTask.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_PREVIOUS ||
                        actionId == EditorInfo.IME_ACTION_NONE ||
                        actionId == EditorInfo.IME_ACTION_UNSPECIFIED
                    ) {
                        nameTask.setBackgroundColor(Color.TRANSPARENT)
                        nameTask.clearFocus()
                        val position = adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            val task = getItem(position)
                            listener.onNameChanged(task, nameTask.text.toString())
                        }

                    }
                    false
                }
                nameTask.doOnTextChanged { text, _, _, _ ->
                    val position = adapterPosition
                    val task = getItem(position)
                    if (!task.checkTask) {
                        if (task.nameTask != text.toString()) {
                            nameTask.setBackgroundColor(Color.GRAY)
                            nameTask.background.alpha = 50

                            Timer().schedule(5000) {
                                if (position != RecyclerView.NO_POSITION) {
                                    listener.onNameChanged(
                                        task,
                                        nameTask.text.toString()
                                    )
                                }
                                nameTask.setBackgroundColor(Color.TRANSPARENT)
                                nameTask.background.alpha = 255
                            }


                        }
                    }
                }
                nameTask.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        nameTaskLayout.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                    }
                }
            }
        }

        fun bind(taskType: TaskType) = with(binding) {
            ViewCompat.setTransitionName(parentLayoutListItem, taskType.idTask.toString())

            nameTask.setText(taskType.nameTask)
            nameTaskLayout.isHintEnabled = false
            checkTask.isChecked = taskType.checkTask
            if (checkTask.isChecked) {
                nameTask.paint.isStrikeThruText = true
                nameTask.isEnabled = false
                parentLayoutListItem.alpha = 0.5F
            } else {
                nameTask.paint.isStrikeThruText = false
                nameTask.isEnabled = true
                parentLayoutListItem.alpha = 1F
                nameTask.setSelection(nameTask.length())
                nameTask.setBackgroundColor(Color.TRANSPARENT)
                when (taskType.priorityTask) {
                    0 -> {
                        parentLayoutListItem.setBackgroundResource(com.example.plannerapp.R.drawable.gradient_priority_leisurely)
                    }
                    1 -> {
                        parentLayoutListItem.setBackgroundResource(com.example.plannerapp.R.drawable.gradient_priority_default)
                    }
                    2 -> {
                        parentLayoutListItem.setBackgroundResource(com.example.plannerapp.R.drawable.gradient_priority_advisable)
                    }
                    3 -> {
                        parentLayoutListItem.setBackgroundResource(com.example.plannerapp.R.drawable.gradient_priority_important)
                    }
                    else -> parentLayoutListItem.setBackgroundResource(com.example.plannerapp.R.drawable.gradient_priority_default)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ListTaskBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(vh: ViewHolder, position: Int) {
        vh.bind(getItem(position))
    }


    interface OnItemClickListener {
        fun onCheckBoxClick(taskType: TaskType, isChecked: Boolean)
        fun onNameChanged(taskType: TaskType, name: String)
    }


    override fun getItemId(position: Int): Long = position.toLong()

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TaskType>() {
            override fun areItemsTheSame(oldItem: TaskType, newItem: TaskType): Boolean {
                return oldItem.idTask == newItem.idTask
            }

            override fun areContentsTheSame(oldItem: TaskType, newItem: TaskType): Boolean {
                return oldItem == newItem
            }

        }
    }
}