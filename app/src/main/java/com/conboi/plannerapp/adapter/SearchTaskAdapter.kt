package com.conboi.plannerapp.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.ListGlobalTaskBinding
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.ui.main.SearchFragmentDirections

class SearchTaskAdapter :
    androidx.recyclerview.widget.ListAdapter<TaskType, SearchTaskAdapter.ViewHolder>(
        SearchTaskDiffCallback()
    ) {

    inner class ViewHolder(private var binding: ListGlobalTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val oldColors: ColorStateList = binding.globalTitleTask.textColors

        init {
            binding.apply {
                parentListGlobalTask.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = getItem(position)
                        val transitionName =
                            root.context.getString(R.string.task_detail_transition_name)
                        val extras = FragmentNavigatorExtras(root to transitionName)
                        val directions =
                            SearchFragmentDirections.actionSearchFragmentToTaskDetailsFragment(
                                idTask = task.idTask
                            )
                        root.findNavController().navigate(directions, extras)
                    }
                }
            }
        }

        fun bind(task: TaskType) = with(binding) {
            binding.task = task
            executePendingBindings()

            subparentListGlobalTask.setPadding(15)
            globalCheckTask.isChecked = task.checked

            if(task.missed){
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

class SearchTaskDiffCallback : DiffUtil.ItemCallback<TaskType>() {
    override fun areItemsTheSame(oldItem: TaskType, newItem: TaskType): Boolean {
        return oldItem.idTask == newItem.idTask
    }

    override fun areContentsTheSame(oldItem: TaskType, newItem: TaskType): Boolean {
        return oldItem == newItem
    }
}