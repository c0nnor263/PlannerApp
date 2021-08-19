package com.conboi.plannerapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.R.drawable
import com.conboi.plannerapp.data.TaskType
import com.conboi.plannerapp.databinding.ListFriendTaskBinding


class FriendTasksAdapter(
    private val tasksList: List<TaskType>
) : ListAdapter<TaskType, FriendTasksAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private var binding: ListFriendTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(taskType: TaskType) = with(binding) {

            nameFriendTask.text = taskType.nameTask
            checkFriendTask.isChecked = taskType.checkTask
            checkFriendTask.isEnabled = false

            if (checkFriendTask.isChecked) {
                nameFriendTask.paint.isStrikeThruText = true
                nameFriendTask.isEnabled = false
                parentLayoutListFriendTask.alpha = 0.5F
                parentLayoutListFriendTask.setBackgroundResource(0)
            } else {
                nameFriendTask.paint.isStrikeThruText = false
                nameFriendTask.isEnabled = true
                parentLayoutListFriendTask.alpha = 1.0F

                when (taskType.priorityTask) {
                    0 -> {
                        parentLayoutListFriendTask.setBackgroundResource(drawable.fragment_water_gradient_priority_leisurely)
                    }
                    1 -> {
                        parentLayoutListFriendTask.setBackgroundResource(drawable.fragment_water_gradient_priority_default)
                    }
                    2 -> {
                        parentLayoutListFriendTask.setBackgroundResource(drawable.fragment_water_gradient_priority_advisable)
                    }
                    3 -> {
                        parentLayoutListFriendTask.setBackgroundResource(drawable.fragment_water_gradient_priority_important)
                    }
                    else -> parentLayoutListFriendTask.setBackgroundResource(drawable.fragment_water_gradient_priority_default)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ListFriendTaskBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(vh: ViewHolder, position: Int) {
        vh.bind(tasksList[position])
    }

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

