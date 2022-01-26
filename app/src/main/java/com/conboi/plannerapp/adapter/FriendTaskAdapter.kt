package com.conboi.plannerapp.adapter

import android.content.res.ColorStateList
import android.graphics.Color
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
        val oldColors: ColorStateList = binding.globalTitleTask.textColors

        fun bind(friendTask: TaskType) = with(binding) {
            task = friendTask
            executePendingBindings()

            globalCheckTask.isChecked = friendTask.checked
            parentListGlobalTask.isClickable = false
            parentListGlobalTask.isFocusable = false
            parentListGlobalTask.isFocusableInTouchMode = false

            if(friendTask.missed){
                globalTitleTask.setTextColor(Color.WHITE)
                totalCheck.setTextColor(Color.WHITE)
            }else{
                globalTitleTask.setTextColor(oldColors)
                totalCheck.setTextColor(oldColors)
            }
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

