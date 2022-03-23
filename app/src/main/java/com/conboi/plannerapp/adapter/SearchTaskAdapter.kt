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
import com.conboi.plannerapp.interfaces.ListInterface

class SearchTaskAdapter(
    private val listener: ListInterface,
    diffCallback: TaskTypeDiffCallback
) : ListAdapter<TaskType, SearchTaskAdapter.ViewHolder>(
        AsyncDifferConfig.Builder(diffCallback).build()
) {

    inner class ViewHolder(private var binding: ListGlobalTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val defaultColors: ColorStateList = binding.tvTitle.textColors

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = getItem(position)
                    listener.onClick(binding.root,task.idTask)
                }
            }

        }

        fun bind(task: TaskType) = with(binding) {
            binding.task = task
            binding.executePendingBindings()
            checkBox.isChecked = task.checked

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