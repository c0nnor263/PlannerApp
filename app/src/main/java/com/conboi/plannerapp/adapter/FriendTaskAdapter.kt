package com.conboi.plannerapp.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.databinding.ListGlobalTaskBinding


class FriendTaskAdapter(
    diffCallback: TaskTypeDiffCallback
) : ListAdapter<TaskType, FriendTaskAdapter.ViewHolder>(
    AsyncDifferConfig.Builder(diffCallback).build()
) {

    inner class ViewHolder(private var binding: ListGlobalTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val defaultColors: ColorStateList = binding.tvTitle.textColors

        fun bind(task: TaskType) = with(binding) {
            binding.task = task
            binding.executePendingBindings()

            checkBox.isChecked = task.checked
            root.isClickable = false
            root.isFocusable = false
            root.isFocusableInTouchMode = false

            if (task.missed) {
                tvTitle.setTextColor(Color.WHITE)
                tvTotalCheck.setTextColor(Color.WHITE)
            } else {
                tvTitle.setTextColor(defaultColors)
                tvTotalCheck.setTextColor(defaultColors)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListGlobalTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(vh: ViewHolder, position: Int) {
        val task = getItem(position)
        vh.bind(task)
    }
}