package com.conboi.plannerapp.adapter

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.databinding.ListTaskBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi


class TaskAdapter @ExperimentalCoroutinesApi constructor(
    private val listener: OnTaskClickListener
) : ListAdapter<TaskType, TaskAdapter.ViewHolder>(TaskDiffCallback()) {
    inner class ViewHolder(private var binding: ListTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var timer: CountDownTimer? = null
        val titleTaskVh: TextInputEditText = binding.titleTask
        var bufferTask: TaskType? = null

        init {
            binding.apply {
                openTask.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = getItem(position)
                        listener.onTitleChanged(task, titleTask.text.toString())
                        listener.onEditTaskCLick(itemView, task)
                    }
                }
                checkTask.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        timer!!.cancel()
                        val task = getItem(position)
                        if (titleTask.text!!.isNotBlank()) {
                            listener.onCheckBoxClick(
                                task.copy(
                                    title = titleTaskVh.text.toString()
                                ), checkTask.isChecked
                            )
                        } else {
                            checkTask.isChecked = false
                            listener.onCheckBoxClick(
                                task.copy(
                                    title = titleTaskVh.text.toString()
                                ), false
                            )

                        }
                    }
                }
                titleTask.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        titleTaskLayout.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                    } else {
                        titleTaskLayout.endIconMode = TextInputLayout.END_ICON_NONE
                    }
                }
            }
        }

        fun bind(taskType: TaskType) = with(binding) {
            bufferTask = taskType
            binding.task = taskType
            binding.executePendingBindings()
            titleTaskLayout.isHintEnabled = false
            checkTask.isChecked = taskType.checked

            titleTask.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE
                ) {
                    timer!!.onFinish()
                    titleTask.clearFocus()
                }
                false
            }
            titleTask.doOnTextChanged { text, _, _, _ ->
                if (!taskType.checked) {
                    if (taskType.title != text.toString()) {
                        timer!!.cancel()
                        timer!!.start()
                    }
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

        //Very important code for some problems
        if (vh.timer != null) {
            vh.timer!!.cancel()
        }

        //CountTimer
        vh.timer = object : CountDownTimer(2000, 2000) {
            override fun onTick(p0: Long) {
                val taskInProcessSavingAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    Color.LTGRAY,
                    Color.TRANSPARENT
                )
                taskInProcessSavingAnimation.duration = 2000
                taskInProcessSavingAnimation.addUpdateListener { animator ->
                    vh.titleTaskVh.setBackgroundColor(
                        animator.animatedValue as Int
                    )
                }
                taskInProcessSavingAnimation.start()
            }

            override fun onFinish() {
                val taskSavedAnimation = ValueAnimator.ofObject(
                    ArgbEvaluator(),
                    Color.GREEN,
                    Color.TRANSPARENT
                )
                taskSavedAnimation.duration = 250
                taskSavedAnimation.addUpdateListener { animator ->
                    vh.titleTaskVh.setBackgroundColor(
                        animator.animatedValue as Int
                    )
                }
                taskSavedAnimation.start()
                listener.onTitleChanged(
                    vh.bufferTask!!,
                    vh.titleTaskVh.text.toString()
                )
                vh.titleTaskVh.setSelection(vh.titleTaskVh.text!!.length)
            }

        }
    }

    interface OnTaskClickListener {
        fun onEditTaskCLick(taskView: View, taskType: TaskType)
        fun onCheckBoxClick(taskType: TaskType, isChecked: Boolean)
        fun onTitleChanged(taskType: TaskType, title: String)
    }

    override fun getItemId(position: Int): Long = position.toLong()
}

class TaskDiffCallback : DiffUtil.ItemCallback<TaskType>() {
    override fun areItemsTheSame(oldItem: TaskType, newItem: TaskType): Boolean {
        return oldItem.idTask == newItem.idTask
    }

    override fun areContentsTheSame(oldItem: TaskType, newItem: TaskType): Boolean {
        return oldItem == newItem
    }

}

