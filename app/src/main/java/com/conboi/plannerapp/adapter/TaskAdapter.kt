package com.conboi.plannerapp.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.*
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.ListTaskBinding
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.parentTask
import com.conboi.plannerapp.utils.setCheckedTotal
import com.conboi.plannerapp.utils.setTotal
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi


class TaskAdapter @ExperimentalCoroutinesApi constructor(
    private val listener: OnTaskClickListener,
) : ListAdapter<TaskType, TaskAdapter.ViewHolder>(
    AsyncDifferConfig.Builder(TaskDiffCallback()).build()
) {
    private var premium = false
    private var middleListTime = GLOBAL_START_DATE

    inner class ViewHolder(var binding: ListTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var titleTimer: CountDownTimer? = null
        var bufferTask: TaskType? = null
        private val oldColors: ColorStateList = binding.title.textColors

        init {
            initClickListener()
        }

        private fun initClickListener() {
            binding.apply {
                openTask.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = getItem(position)
                        titleTimer!!.cancel()
                        openTask.visibility = View.VISIBLE
                        savingTitleIndicator.visibility = View.GONE
                        (savingTitleIndicator.drawable as AnimatedVectorDrawable).stop()
                        if (title.text.toString() != bufferTask!!.title) {
                            listener.onTitleChanged(task, title.text.toString())
                        }
                        listener.onEditTaskCLick(task, itemView)
                    }
                }
                checkTask.setOnClickListener {
                    complexCheckTask(true)
                }
                checkTask.setOnLongClickListener {
                    complexCheckTask(false)
                    true
                }
                title.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        titleLayout.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                    } else {
                        titleLayout.endIconMode = TextInputLayout.END_ICON_NONE
                    }
                }
            }
        }

        private fun complexCheckTask(one: Boolean) = with(binding) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                titleTimer!!.cancel()
                openTask.visibility = View.VISIBLE
                savingTitleIndicator.visibility = View.GONE
                (savingTitleIndicator.drawable as AnimatedVectorDrawable).stop()
                val task = getItem(position)
                if (task.totalChecked > 1) {
                    checkTask.isChecked = true
                }
                if (title.text!!.isNotBlank()) {
                    //Title is not empty
                    if (one) {
                        listener.onCheckBoxEvent(
                            task.copy(
                                title = title.text.toString()
                            ),
                            isChecked = checkTask.isChecked,
                            isHold = false,
                            task.lastOvercheck
                        )
                    } else {
                        listener.onCheckBoxEvent(
                            task.copy(
                                title = title.text.toString()
                            ),
                            isChecked = true,
                            isHold = true,
                            task.lastOvercheck
                        )
                    }
                } else {
                    //Title is empty
                    checkTask.isChecked = false
                    if (one) {
                        listener.onCheckBoxEvent(
                            task.copy(
                                title = title.text.toString()
                            ),
                            isChecked = false,
                            isHold = false,
                            task.lastOvercheck
                        )
                    } else {
                        listener.onCheckBoxEvent(
                            task.copy(
                                title = title.text.toString()
                            ),
                            isChecked = false,
                            isHold = true,
                            task.lastOvercheck
                        )
                    }
                }
            } else {
                return@with
            }

        }

        fun bind(task: TaskType, isPremium: Boolean) = with(binding) {
            bufferTask = task
            setTask(task)
            executePendingBindings()

            openTask.visibility = View.VISIBLE
            openTask.alpha = 0.5f
            savingTitleIndicator.visibility = View.GONE

            if (bufferTask!!.missed) {
                title.setTextColor(Color.WHITE)
                totalCheck.setTextColor(Color.WHITE)
            } else {
                title.setTextColor(oldColors)
                totalCheck.setTextColor(oldColors)
            }

            if (isPremium) {
                title.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE
                    ) {
                        titleTimer!!.onFinish()
                        (savingTitleIndicator.drawable as AnimatedVectorDrawable).stop()
                        savingTitleIndicator.visibility = View.GONE
                        openTask.visibility = View.VISIBLE
                        openTask.alpha = 0.5f
                        title.clearFocus()
                    }
                    false
                }
                title.doOnTextChanged { text, _, _, _ ->
                    (savingTitleIndicator.drawable as AnimatedVectorDrawable).stop()
                    titleTimer!!.cancel()
                    titleTimer!!.start()
                    savingTitleIndicator.setImageResource(R.drawable.saving_anim)
                    (savingTitleIndicator.drawable as AnimatedVectorDrawable).start()
                    if (text?.isEmpty() != false) {
                        subparentListTask.setBackgroundResource(R.color.secondaryDarkColorWater)
                    }
                }

                title.isFocusable = true
                openTask.isFocusable = true
                checkTask.isFocusable = true
                initClickListener()
            } else {
                title.isFocusable = false
                openTask.isFocusable = false
                checkTask.isFocusable = false
                subparentListTask.alpha = 0.5F

                openTask.setOnClickListener { listener.promptPremium() }
                title.setOnClickListener { listener.promptPremium() }
                checkTask.setOnClickListener {
                    listener.promptPremium()
                    checkTask.isChecked = false
                }
            }
        }

        fun bindPayload(task: TaskType, payloads: MutableList<Any>) {
            binding.apply {
                val newTitle = payloads.first() as String
                subparentListTask.parentTask(
                    task.priority,
                    task.checked,
                    newTitle,
                    task.totalChecked,
                    task.missed
                )
                checkTask.isChecked = task.checked
                totalCheck.setTotal(task.totalChecked)
                setCheckedTotal(title as View, task.checked, task.totalChecked)

                if (bufferTask!!.missed) {
                    title.setTextColor(Color.WHITE)
                    totalCheck.setTextColor(Color.WHITE)
                } else {
                    title.setTextColor(oldColors)
                    totalCheck.setTextColor(oldColors)
                }

                if (!title.isFocused) {
                    title.setText(newTitle)
                    savingTitleIndicator.visibility = View.GONE
                    openTask.visibility = View.VISIBLE
                    openTask.alpha = 0.5f
                }
                if (titleTimer != null) {
                    titleTimer!!.cancel()
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: TaskAdapter.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNullOrEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.bindPayload(currentList[position], payloads)
        }
    }

    override fun onBindViewHolder(vh: TaskAdapter.ViewHolder, position: Int) {
        val task = getItem(position)
        if (!premium &&
            currentList.size > 50 &&
            task.created >= middleListTime &&
            middleListTime != GLOBAL_START_DATE
        ) {
            vh.bind(task, false)
        } else {
            vh.bind(task, true)
        }

        if (vh.titleTimer != null) {
            vh.titleTimer!!.cancel()
        }
        vh.binding.apply {
            vh.titleTimer = object : CountDownTimer(3000, 1000) {
                override fun onTick(remainingTime: Long) {
                    openTask.visibility = View.GONE
                    savingTitleIndicator.visibility = View.VISIBLE
                    if (remainingTime <= 1000 && remainingTime != 0.toLong()) {
                        (savingTitleIndicator.drawable as AnimatedVectorDrawable).stop()
                        savingTitleIndicator.setImageResource(R.drawable.saving_done)
                        (savingTitleIndicator.drawable as AnimatedVectorDrawable).start()
                    }
                }

                override fun onFinish() {
                    (savingTitleIndicator.drawable as AnimatedVectorDrawable).stop()
                    savingTitleIndicator.visibility = View.GONE
                    openTask.visibility = View.VISIBLE
                    openTask.alpha = 0.5f
                    listener.onTitleChanged(
                        vh.bufferTask!!,
                        title.text.toString()
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskAdapter.ViewHolder {
        return ViewHolder(
            ListTaskBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    fun updatePremiumState(state: Boolean) {
        premium = state
    }

    fun updateMiddleTime(middleTime: Long) {
        middleListTime = middleTime
    }

    interface OnTaskClickListener {
        fun onEditTaskCLick(task: TaskType, taskView: View)
        fun onCheckBoxEvent(
            task: TaskType,
            isChecked: Boolean,
            isHold: Boolean,
            lastOverchecked: Long
        )

        fun onTitleChanged(task: TaskType, title: String)
        fun promptPremium()
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

    override fun getChangePayload(oldItem: TaskType, newItem: TaskType): Any? {
        if (oldItem.title != newItem.title) return newItem.title
        return super.getChangePayload(oldItem, newItem)
    }
}

