package com.conboi.plannerapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.databinding.ListGlobalTaskBinding
import com.conboi.plannerapp.model.TaskType


class FriendTasksAdapter :
    ListAdapter<TaskType, FriendTasksAdapter.ViewHolder>(FriendTasksDiffCallBack()) {

    inner class ViewHolder(private var binding: ListGlobalTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(friendTask: TaskType) = with(binding) {
            task = friendTask
            globalCheckTask.isChecked = friendTask.checked
            globalCheckTask.isClickable = false
            executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ListGlobalTaskBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(vh: ViewHolder, position: Int) {
        vh.bind(getItem(position))
    }

}

class FriendTasksDiffCallBack : DiffUtil.ItemCallback<TaskType>() {
    override fun areItemsTheSame(oldItem: TaskType, newItem: TaskType): Boolean {
        return oldItem.idTask == newItem.idTask
    }

    override fun areContentsTheSame(oldItem: TaskType, newItem: TaskType): Boolean {
        return oldItem == newItem
    }

}

