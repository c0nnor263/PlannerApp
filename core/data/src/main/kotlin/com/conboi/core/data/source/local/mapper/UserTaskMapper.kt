package com.conboi.core.data.source.local.mapper

import com.conboi.core.database.entity.UserTask
import com.conboi.core.domain.mapper.Mapper
import com.conboi.core.domain.source.local.model.UserTaskDTO
import javax.inject.Inject

class UserTaskMapper
@Inject
constructor() : Mapper<UserTask, UserTaskDTO> {
    override fun from(source: UserTask): UserTaskDTO {
        return UserTaskDTO(
            id = source.id,
            title = source.title,
            description = source.description,
            priority = source.priority,
            createdTime = source.createdTime,
            reminderTime = source.reminderTime,
            deadlineTime = source.deadlineTime,
            completedTime = source.completedTime,
            lastMultipleCompleteTime = source.lastMultipleCompleteTime,
            reminderRepeatMode = source.reminderRepeatMode,
            isMissed = source.isMissed,
            isCompleted = source.isCompleted,
            totalCompleted = source.totalCompleted
        )
    }

    override fun to(destination: UserTaskDTO): UserTask {
        return UserTask(
            id = destination.id,
            title = destination.title,
            description = destination.description,
            priority = destination.priority,
            createdTime = destination.createdTime,
            reminderTime = destination.reminderTime,
            deadlineTime = destination.deadlineTime,
            completedTime = destination.completedTime,
            lastMultipleCompleteTime = destination.lastMultipleCompleteTime,
            reminderRepeatMode = destination.reminderRepeatMode,
            isMissed = destination.isMissed,
            isCompleted = destination.isCompleted,
            totalCompleted = destination.totalCompleted
        )
    }
}