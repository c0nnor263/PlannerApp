package com.conboi.plannerapp.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.databinding.ListTaskBinding
import com.conboi.plannerapp.interfaces.TaskListInterface
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.MIDDLE_COUNT


class TaskAdapter(
    private val listener: TaskListInterface,
    diffCallback: TaskTypeDiffCallback
) : ListAdapter<TaskType, TaskAdapter.ViewHolder>(
    AsyncDifferConfig.Builder(diffCallback).build()
) {
    private var premium = false
    private var middleListTime = GLOBAL_START_DATE

    inner class ViewHolder(var binding: ListTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val defaultColors: ColorStateList = binding.tietTitle.textColors
        var titleTimer: CountDownTimer? = null
        var bufferTask: TaskType? = null

        fun bind(task: TaskType, isPremium: Boolean) = with(binding) {
            bufferTask = task
            binding.task = task
            binding.executePendingBindings()

            idleAnimationState()

            if (isPremium) premiumUI() else nonPremiumUI()
        }

        fun bindPayload(task: TaskType, payloads: MutableList<Any>) {
            binding.checkBox.isChecked = task.checked

            val newTitle = payloads.first() as String
            bind(task.copy(title = newTitle), true)

            if (!binding.tietTitle.isFocused) {
                binding.tietTitle.setText(newTitle)
                idleAnimationState()
            }
        }


        private fun complexCheckTask(hold: Boolean) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                instantSave()

                val titleString = binding.tietTitle.text.toString()

                val task = getItem(position)
                if (task.totalChecked > 1) {
                    binding.checkBox.isChecked = true
                }
                if (titleString.isNotBlank()) {
                    //Title is not empty
                    if (hold) {
                        listener.onCheckBoxEvent(
                            task.copy(title = titleString),
                            isChecked = hold,
                            isHold = hold,
                        )
                    } else {
                        listener.onCheckBoxEvent(
                            task.copy(title = titleString),
                            isChecked = binding.checkBox.isChecked,
                            isHold = hold,
                        )
                    }
                } else {
                    //Title is empty
                    binding.checkBox.isChecked = false
                    listener.onCheckBoxEvent(
                        task.copy(title = titleString),
                        isChecked = false,
                        isHold = false,
                    )
                }
            }
        }

        //Default animation state for UI
        fun idleAnimationState() {
            checkMissedState()
            binding.ivSavingTitleIndicator.apply {
                setImageResource(R.drawable.saving_anim)
                (drawable as AnimatedVectorDrawable).stop()
                visibility = View.GONE
            }

            binding.ivBtnOpenTask.visibility = View.VISIBLE
        }

        //Checking for missed state
        private fun checkMissedState() = with(binding) {
            if (bufferTask?.missed == true) {
                tietTitle.setTextColor(Color.WHITE)
                tvTotalCheck.setTextColor(Color.WHITE)
            } else {
                tietTitle.setTextColor(defaultColors)
                tvTotalCheck.setTextColor(defaultColors)
            }
        }

        //Saving values
        private fun instantSave() {
            titleTimer?.onFinish()
            idleAnimationState()
        }

        private fun resetSavingTimer() {
            titleTimer?.cancel()
            titleTimer?.start()
            binding.ivSavingTitleIndicator.setImageResource(R.drawable.saving_anim)
            (binding.ivSavingTitleIndicator.drawable as AnimatedVectorDrawable).start()
        }


        //PremiumUIs
        private fun premiumUI() = with(binding) {
            tietTitle.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE
                ) {
                    instantSave()
                    tietTitle.clearFocus()
                }
                false
            }
            tietTitle.doOnTextChanged { text, _, _, _ ->
                resetSavingTimer()
                if (text?.isEmpty() == true) {
                    clSubparent.setBackgroundResource(R.color.secondaryDarkColorWater)
                }
            }
            /*tietTitle.setOnFocusChangeListener { _, hasFocus ->
                tilTitle.endIconMode = if (hasFocus) {
                    TextInputLayout.END_ICON_CLEAR_TEXT
                } else {
                    TextInputLayout.END_ICON_NONE
                }
            }*/

            //ImageButton
            ivBtnOpenTask.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    instantSave()
                    val task = getItem(position)
                    listener.onClick(root, task.idTask)
                }
            }

            //CheckBox
            checkBox.setOnClickListener {
                complexCheckTask(false)
            }
            checkBox.setOnLongClickListener {
                complexCheckTask(true)
                true
            }


            tietTitle.isFocusable = true
            ivBtnOpenTask.isFocusable = true
            checkBox.isFocusable = true
            clSubparent.alpha = 1.0F
        }

        private fun nonPremiumUI() = with(binding) {
            tietTitle.setOnClickListener { listener.showPremiumDealDialog() }

            //ImageButton
            ivBtnOpenTask.setOnClickListener { listener.showPremiumDealDialog() }

            //CheckBox
            checkBox.setOnClickListener {
                listener.showPremiumDealDialog()
                checkBox.isChecked = false
            }
            checkBox.setOnLongClickListener {
                listener.showPremiumDealDialog()
                checkBox.isChecked = false
                true
            }

            tietTitle.isFocusable = false
            ivBtnOpenTask.isFocusable = false
            checkBox.isFocusable = false
            clSubparent.alpha = 0.5F
        }

    }

    override fun onBindViewHolder(vh: TaskAdapter.ViewHolder, position: Int) {
        val task = getItem(position)

        //Check premium state
        val isNotPremium = !premium &&
                currentList.size > MIDDLE_COUNT &&
                task.created >= middleListTime &&
                middleListTime != GLOBAL_START_DATE

        vh.bind(task, !isNotPremium)

        if (vh.titleTimer != null) {
            vh.titleTimer?.cancel()
        }

        vh.binding.apply {
            fun runSaveAnimation() {
                ivSavingTitleIndicator.setImageResource(R.drawable.saving_done)
                (ivSavingTitleIndicator.drawable as AnimatedVectorDrawable).start()
            }

            vh.titleTimer = object : CountDownTimer(3000, 1000) {
                override fun onTick(remainingTime: Long) {
                    ivBtnOpenTask.visibility = View.GONE
                    ivSavingTitleIndicator.visibility = View.VISIBLE
                    if (remainingTime <= 1000 && remainingTime != 0.toLong()) {
                        runSaveAnimation()
                    }
                }

                override fun onFinish() {
                    val titleString = tietTitle.text.toString()
                    vh.idleAnimationState()

                    if (titleString != vh.bufferTask?.title) {
                        listener.onTitleChanged(vh.bufferTask!!, titleString)
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(
        vh: TaskAdapter.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(vh, position, payloads)
        } else {
            val task = currentList[position]
            vh.bindPayload(task, payloads)

            if (vh.titleTimer != null) {
                vh.titleTimer?.cancel()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onViewRecycled(holder: ViewHolder) {
        holder.titleTimer?.onFinish()
        super.onViewRecycled(holder)
    }

    override fun getItemId(position: Int): Long = position.toLong()

    fun updatePremiumState(state: Boolean) {
        premium = state
    }

    fun updateMiddleTime(middleTime: Long) {
        middleListTime = middleTime
    }
}