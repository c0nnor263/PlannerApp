package com.conboi.plannerapp.data.model

import android.os.Parcelable
import android.provider.BaseColumns
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.conboi.plannerapp.data.model.TaskType.TaskEntry.COLUMN_CHECKED
import com.conboi.plannerapp.data.model.TaskType.TaskEntry.COLUMN_COMPLETED
import com.conboi.plannerapp.data.model.TaskType.TaskEntry.COLUMN_CREATED
import com.conboi.plannerapp.data.model.TaskType.TaskEntry.COLUMN_DESCRIPTION
import com.conboi.plannerapp.data.model.TaskType.TaskEntry.COLUMN_TITLE
import com.conboi.plannerapp.data.model.TaskType.TaskEntry.COLUMN_PRIORITY
import com.conboi.plannerapp.data.model.TaskType.TaskEntry.COLUMN_TIME
import com.conboi.plannerapp.data.model.TaskType.TaskEntry.TABLE_NAME
import com.conboi.plannerapp.utils.GLOBAL_DATE_FOR_CHECK
import kotlinx.android.parcel.Parcelize
import java.text.DateFormat

@Entity(tableName = TABLE_NAME)
@Parcelize
@Keep
data class TaskType(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var idTask: Int = 0,
    @ColumnInfo(name = COLUMN_TITLE) val title: String?,
    @ColumnInfo(name = COLUMN_DESCRIPTION) val description: String?,
    @ColumnInfo(name = COLUMN_TIME) val time: Int = GLOBAL_DATE_FOR_CHECK,
    @ColumnInfo(name = COLUMN_PRIORITY) val priority: Int = 1,
    @ColumnInfo(name = COLUMN_CHECKED) val checked: Boolean = false,
    @ColumnInfo(name = COLUMN_CREATED) val created: Long = System.currentTimeMillis(),
    @ColumnInfo(name = COLUMN_COMPLETED) val completed: Long = 0
) : Parcelable {
    val createdFormatted: String get() = DateFormat.getDateTimeInstance().format(created)
    val completedFormatted: String
        get() = DateFormat.getDateTimeInstance().format(completed)

    object TaskEntry : BaseColumns {
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_TIME = "time"
        const val COLUMN_PRIORITY = "priority"
        const val COLUMN_CHECKED = "checked"
        const val COLUMN_CREATED = "created"
        const val COLUMN_COMPLETED = "completed"


        const val DATABASE_NAME = "TaskList.db"
        const val TABLE_NAME = "UserTasks"

    }
}