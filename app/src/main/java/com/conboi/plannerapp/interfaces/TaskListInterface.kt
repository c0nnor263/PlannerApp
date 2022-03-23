package com.conboi.plannerapp.interfaces

import com.conboi.plannerapp.data.model.TaskType

interface TaskListInterface:ListInterface {
    fun onCheckBoxEvent(
        task: TaskType,
        isChecked: Boolean,
        isHold: Boolean,
    )

    fun onTitleChanged(task: TaskType, title: String)
    fun showPremiumDealDialog()
}