package com.conboi.plannerapp.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.data.model.TaskType.TaskEntry.COLUMN_CHECKED
import com.conboi.plannerapp.data.model.TaskType.TaskEntry.TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    fun getTasks(
        searchQuery: String,
        sortOrder: SortOrder,
        hideCompleted: Boolean
    ): Flow<List<TaskType>> =
        when (sortOrder) {
            SortOrder.BY_TITLE -> getTasksSortByName(searchQuery, hideCompleted)
            SortOrder.BY_DATE -> getTasksSortByCreatedDate(searchQuery, hideCompleted)
            SortOrder.BY_COMPLETE -> getTasksSortByCompletedDate(searchQuery, hideCompleted)
        }

    @Query("SELECT * FROM $TABLE_NAME WHERE (:hideCompleted != checked or $COLUMN_CHECKED = 0) AND title LIKE '%' || :searchQuery || '%' ORDER BY priority DESC, title")
    fun getTasksSortByName(searchQuery: String, hideCompleted: Boolean): Flow<List<TaskType>>

    @Query("SELECT * FROM $TABLE_NAME WHERE (:hideCompleted != checked or $COLUMN_CHECKED = 0) AND title LIKE '%' || :searchQuery || '%' ORDER BY priority DESC, created DESC")
    fun getTasksSortByCreatedDate(searchQuery: String, hideCompleted: Boolean): Flow<List<TaskType>>

    @Query("SELECT * FROM $TABLE_NAME WHERE (:hideCompleted != checked or $COLUMN_CHECKED = 0) AND title LIKE '%' || :searchQuery || '%' ORDER BY completed DESC, priority DESC")
    fun getTasksSortByCompletedDate(
        searchQuery: String,
        hideCompleted: Boolean
    ): Flow<List<TaskType>>


    @Query("SELECT COUNT(*) FROM $TABLE_NAME")
    fun getTasksSize(): LiveData<Int>

    @Query("SELECT COUNT(*) from  $TABLE_NAME WHERE $COLUMN_CHECKED == 1")
    fun getTasksCheckedCount(): LiveData<Int>

    @Query("SELECT * from  $TABLE_NAME WHERE id = :id")
    fun getTask(id: Int): Flow<TaskType>

    @Query("DELETE FROM $TABLE_NAME WHERE $COLUMN_CHECKED == 1")
    suspend fun deleteCompletedTasks()

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun signOutDeleteTasks()


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taskType: TaskType)

    @Update
    suspend fun update(taskType: TaskType)

    @Delete
    suspend fun delete(taskType: TaskType)


}