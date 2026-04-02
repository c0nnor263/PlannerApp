package com.conboi.core.domain.old.interfaces

import com.conboi.core.domain.source.local.model.UserTaskDTO
import com.conboi.plannerapp.interfaces.ListInterface

interface TaskListInterface : ListInterface {
    fun onCheckBoxEvent(
        task: UserTaskDTO,
        isChecked: Boolean,
        isHold: Boolean
    )

    fun onTitleChanged(
        task: UserTaskDTO,
        title: String
    )

    fun showPremiumDealDialog()
}