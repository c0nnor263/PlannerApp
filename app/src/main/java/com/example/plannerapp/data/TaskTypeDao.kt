package com.example.plannerapp.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.plannerapp.data.TaskType.TaskEntry.COLUMN_CHECKED
import com.example.plannerapp.data.TaskType.TaskEntry.TABLE_NAME
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTypeDao {
    fun getTasks(searchQuery:String, sortOrder: SortOrder, hideCompleted: Boolean):Flow<List<TaskType>> =
        when(sortOrder){
            SortOrder.BY_NAME -> getTasksSortByName(searchQuery,hideCompleted)
            SortOrder.BY_DATE -> getTasksSortByCreatedDate(searchQuery, hideCompleted)
            SortOrder.BY_COMPLETED -> getTasksSortByCompletedDate(searchQuery,hideCompleted)
        }

    @Query("SELECT * FROM $TABLE_NAME WHERE ($COLUMN_CHECKED != :hideCompleted OR $COLUMN_CHECKED = 0) AND name_task LIKE '%' || :searchQuery || '%' ORDER BY isCheck ASC, priority_of_task DESC, name_task")
    fun getTasksSortByName(searchQuery:String, hideCompleted:Boolean): Flow<List<TaskType>>

    @Query("SELECT * FROM $TABLE_NAME WHERE ($COLUMN_CHECKED != :hideCompleted OR $COLUMN_CHECKED = 0) AND name_task LIKE '%' || :searchQuery || '%' ORDER BY isCheck ASC, priority_of_task DESC, created")
    fun getTasksSortByCreatedDate(searchQuery:String, hideCompleted:Boolean): Flow<List<TaskType>>

    @Query("SELECT * FROM $TABLE_NAME WHERE ($COLUMN_CHECKED != :hideCompleted OR $COLUMN_CHECKED = 0) AND name_task LIKE '%' || :searchQuery || '%' ORDER BY isCheck DESC, completed DESC")
    fun getTasksSortByCompletedDate(searchQuery:String, hideCompleted:Boolean): Flow<List<TaskType>>



    @Query("SELECT COUNT(*) FROM $TABLE_NAME")
    fun getTasksSize(): LiveData<Int>

    @Query("SELECT COUNT(*) from  $TABLE_NAME WHERE $COLUMN_CHECKED == 1")
    fun getTasksCheckedCount(): LiveData<Int>

    @Query("SELECT * from  $TABLE_NAME WHERE idTask = :id")
    fun getTask(id: Int): Flow<TaskType>




    @Query("DELETE FROM $TABLE_NAME WHERE $COLUMN_CHECKED == 1")
    suspend fun deleteCompletedTasks()

    @Insert
    suspend fun insert(taskType: TaskType)

    @Update
    suspend fun update(taskType: TaskType)

    @Delete
    suspend fun delete(taskType: TaskType)

}