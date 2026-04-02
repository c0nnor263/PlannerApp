package com.conboi.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.conboi.core.database.entity.UserTask
import com.conboi.core.domain.old.enums.SortOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTaskDao {
    fun getSortedTasks(
        searchQuery: String,
        sortOrder: SortOrder,
        hideCompleted: Boolean,
        hideMultipleCompleted: Boolean
    ): Flow<List<UserTask>> =
        when (sortOrder) {
            SortOrder.BY_TITLE ->
                getTasksSortByName(
                    searchQuery,
                    hideCompleted,
                    hideMultipleCompleted
                )

            SortOrder.BY_DATE ->
                getTasksSortByCreatedDate(
                    searchQuery,
                    hideCompleted,
                    hideMultipleCompleted
                )

            SortOrder.BY_COMPLETE ->
                getTasksSortByCompletedDate(
                    searchQuery,
                    hideCompleted,
                    hideMultipleCompleted
                )

            SortOrder.BY_MULTIPLE_COMPLETED ->
                getTasksSortByMultipleCompleted(
                    searchQuery,
                    hideCompleted,
                    hideMultipleCompleted
                )
        }

    @Query(
        "SELECT * FROM usertask WHERE (:hideMultipleCompleted != (totalCompleted > 1) or totalCompleted < 2) AND (:hideCompleted != isCompleted or (isCompleted = 0 or totalCompleted > 1)) AND title LIKE '%' || :searchQuery || '%' ORDER BY priority DESC, title"
    )
    fun getTasksSortByName(
        searchQuery: String,
        hideCompleted: Boolean,
        hideMultipleCompleted: Boolean
    ): Flow<List<UserTask>>

    @Query(
        "SELECT * FROM usertask WHERE (:hideMultipleCompleted != (totalCompleted > 1) or totalCompleted < 2) AND (:hideCompleted != isCompleted or (isCompleted = 0 or totalCompleted > 1)) AND title LIKE '%' || :searchQuery || '%' ORDER BY priority DESC, createdTime DESC"
    )
    fun getTasksSortByCreatedDate(
        searchQuery: String,
        hideCompleted: Boolean,
        hideMultipleCompleted: Boolean
    ): Flow<List<UserTask>>

    @Query(
        "SELECT * FROM usertask WHERE (:hideMultipleCompleted != (totalCompleted > 1) or totalCompleted < 2) AND (:hideCompleted != isCompleted or (isCompleted = 0 or totalCompleted > 1)) AND title LIKE '%' || :searchQuery || '%' ORDER BY isCompleted, priority DESC"
    )
    fun getTasksSortByCompletedDate(
        searchQuery: String,
        hideCompleted: Boolean,
        hideMultipleCompleted: Boolean
    ): Flow<List<UserTask>>

    @Query(
        "SELECT * FROM usertask WHERE (:hideMultipleCompleted != (totalCompleted > 1) or totalCompleted < 2) AND (:hideCompleted != isCompleted or (isCompleted = 0 or totalCompleted > 1)) AND title LIKE '%' || :searchQuery || '%' ORDER BY totalCompleted DESC, priority DESC, title"
    )
    fun getTasksSortByMultipleCompleted(
        searchQuery: String,
        hideCompleted: Boolean,
        hideMultipleCompleted: Boolean
    ): Flow<List<UserTask>>

    @Query("SELECT * from usertask WHERE id == :id")
    fun getTask(id: Int): Flow<UserTask>

    @Query("SELECT * FROM usertask")
    fun getAllTasks(): Flow<List<UserTask>>

    @Query("SELECT * from usertask WHERE isCompleted == 1 AND totalCompleted < 2")
    fun getCompletedTasks(): Flow<List<UserTask>>

    @Query("SELECT * from usertask WHERE totalCompleted > 1")
    fun getMultipleCompletedTasks(): Flow<List<UserTask>>

    @Query("SELECT COUNT(*) FROM usertask")
    fun getTaskSize(): Flow<Int>

    @Query("SELECT COUNT(*) from usertask WHERE isCompleted == 1 AND totalCompleted < 2")
    fun getCompletedTaskSize(): Flow<Int>

    @Query("SELECT COUNT(*) from usertask WHERE totalCompleted > 1")
    fun getMultipleCompletedTaskSize(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(userTask: UserTask)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(userTask: List<UserTask>)

    @Update
    suspend fun update(userTask: UserTask)

    @Delete
    suspend fun delete(userTask: UserTask)

    @Query("DELETE FROM usertask WHERE isCompleted == 1 AND totalCompleted < 2")
    suspend fun deleteCompletedTasks()

    @Query("DELETE FROM usertask WHERE totalCompleted > 1")
    suspend fun deleteMultipleCompletedTasks()

    @Query("DELETE FROM usertask")
    suspend fun deleteAllTasks()
}