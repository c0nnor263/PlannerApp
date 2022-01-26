package com.conboi.plannerapp.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.conboi.plannerapp.model.TaskType.Companion.TABLE_NAME
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import kotlinx.parcelize.Parcelize

@Entity(tableName = TABLE_NAME)
@Parcelize
@Keep
data class TaskType(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val idTask: Int = 0,
    @ColumnInfo(name = COLUMN_TITLE) val title: String = "",
    @ColumnInfo(name = COLUMN_DESCRIPTION) val description: String = "",
    @ColumnInfo(name = COLUMN_PRIORITY) val priority: Int = 1,

    @ColumnInfo(name = COLUMN_TIME) var time: Long = GLOBAL_START_DATE,
    @ColumnInfo(name = COLUMN_DEADLINE) val deadline: Long = GLOBAL_START_DATE,
    @ColumnInfo(name = COLUMN_LAST_OVERCHECK) val lastOvercheck:Long = GLOBAL_START_DATE,
    @ColumnInfo(name = COLUMN_CREATED) val created: Long = System.currentTimeMillis(),
    @ColumnInfo(name = COLUMN_COMPLETED) val completed: Long = GLOBAL_START_DATE,

    @ColumnInfo(name = COLUMN_REPEAT_MODE) val repeatMode: Int = 0,
    @ColumnInfo(name = COLUMN_MISSED) var missed: Boolean = false,
    @ColumnInfo(name = COLUMN_CHECKED) val checked: Boolean = false,
    @ColumnInfo(name = COLUMN_TOTAL_CHECKED) val totalChecked: Int = 0,
) : Parcelable {

    companion object{
        const val COLUMN_ID = "idTask"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_PRIORITY = "priority"

        const val COLUMN_TIME = "time"
        const val COLUMN_DEADLINE = "deadline"
        const val COLUMN_LAST_OVERCHECK = "lastOvercheck"
        const val COLUMN_CREATED = "created"
        const val COLUMN_COMPLETED = "completed"

        const val COLUMN_REPEAT_MODE = "repeatMode"
        const val COLUMN_MISSED = "missed"
        const val COLUMN_CHECKED = "checked"
        const val COLUMN_TOTAL_CHECKED = "totalChecked"


        const val DATABASE_NAME = "PlannerAppDB.db"
        const val TABLE_NAME = "UserTasks"
    }
}