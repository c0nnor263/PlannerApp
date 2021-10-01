package com.conboi.plannerapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.databinding.ListGlobalTaskBinding


class FriendTasksAdapter(
    private val taskList: List<TaskType>
) : ListAdapter<TaskType, FriendTasksAdapter.ViewHolder>(FriendTasksDiffCallBack()) {

    inner class ViewHolder(private var binding: ListGlobalTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(taskType: TaskType) = with(binding) {
            task = taskType
            executePendingBindings()
            globalCheckTask.isEnabled = false
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
        vh.bind(taskList[position])
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

