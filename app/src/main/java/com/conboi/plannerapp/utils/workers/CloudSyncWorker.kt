package com.conboi.plannerapp.utils.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.conboi.plannerapp.data.TaskDao
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val taskDao: TaskDao
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        WorkManager.getInstance(applicationContext).pruneWork()
        val userTaskListReference =
            FirebaseFirestore.getInstance()
                .document("Users/${Firebase.auth.currentUser!!.uid}/TaskList/Tasks")
        var result: Result = Result.success()
        val tasks = taskDao.getAllTasks().first()

        if (tasks.isNotEmpty()) {
            userTaskListReference
                .get()
                .addOnCompleteListener { userTaskList ->
                    if (userTaskList.isSuccessful) {
                        userTaskListReference.set(
                            tasks.associateBy({ it.idTask.toString() }, { it })
                        )
                            .addOnSuccessListener {
                                result = Result.success()
                            }
                            .addOnFailureListener {
                                result = Result.failure()
                            }
                    } else {
                        result = Result.success()
                    }
                }
        }
        return result
    }

}