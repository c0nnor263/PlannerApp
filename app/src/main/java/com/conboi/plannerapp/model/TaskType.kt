package com.conboi.plannerapp.model

import android.os.Parcelable
import android.provider.BaseColumns
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.conboi.plannerapp.model.TaskType.TaskEntry.COLUMN_CHECKED
import com.conboi.plannerapp.model.TaskType.TaskEntry.COLUMN_COMPLETED
import com.conboi.plannerapp.model.TaskType.TaskEntry.COLUMN_CREATED
import com.conboi.plannerapp.model.TaskType.TaskEntry.COLUMN_DEADLINE
import com.conboi.plannerapp.model.TaskType.TaskEntry.COLUMN_DESCRIPTION
import com.conboi.plannerapp.model.TaskType.TaskEntry.COLUMN_PRIORITY
import com.conboi.plannerapp.model.TaskType.TaskEntry.COLUMN_REPEAT_MODE
import com.conboi.plannerapp.model.TaskType.TaskEntry.COLUMN_TIME
import com.conboi.plannerapp.model.TaskType.TaskEntry.COLUMN_TITLE
import com.conboi.plannerapp.model.TaskType.TaskEntry.COLUMN_TOTAL_CHECKED
import com.conboi.plannerapp.model.TaskType.TaskEntry.TABLE_NAME
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import kotlinx.parcelize.Parcelize
import java.text.DateFormat

@Entity(tableName = TABLE_NAME)
@Parcelize
@Keep
data class TaskType(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var idTask: Int = 0,
    @ColumnInfo(name = COLUMN_TITLE) val title: String = "",
    @ColumnInfo(name = COLUMN_DESCRIPTION) val description: String = "",
    @ColumnInfo(name = COLUMN_TIME) val time: Long = GLOBAL_START_DATE,
    @ColumnInfo(name = COLUMN_REPEAT_MODE) val repeatMode: Int = 0,
    @ColumnInfo(name = COLUMN_DEADLINE) val deadline: Long = GLOBAL_START_DATE,
    @ColumnInfo(name = COLUMN_PRIORITY) val priority: Int = 1,
    @ColumnInfo(name = COLUMN_CHECKED) val checked: Boolean = false,
    @ColumnInfo(name = COLUMN_TOTAL_CHECKED) val totalChecked: Int = 0,
    @ColumnInfo(name = COLUMN_CREATED) val created: Long = System.currentTimeMillis(),
    @ColumnInfo(name = COLUMN_COMPLETED) val completed: Long = 0
) : Parcelable {
    val createdFormatted: String get() = DateFormat.getDateTimeInstance().format(created)

    object TaskEntry : BaseColumns {
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_TIME = "time"
        const val COLUMN_REPEAT_MODE = "repeatMode"
        const val COLUMN_DEADLINE = "deadline"
        const val COLUMN_PRIORITY = "priority"
        const val COLUMN_CHECKED = "checked"
        const val COLUMN_TOTAL_CHECKED = "totalChecked"
        const val COLUMN_CREATED = "created"
        const val COLUMN_COMPLETED = "completed"

        const val DATABASE_NAME = "PlannerAppDB.db"
        const val TABLE_NAME = "UserTasks"

    }
}