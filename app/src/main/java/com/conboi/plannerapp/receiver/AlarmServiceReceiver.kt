package com.conboi.plannerapp.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.TaskDao
import com.conboi.plannerapp.data.dataStore
import com.conboi.plannerapp.model.TaskType
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.myclass.AlarmMethods
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmServiceReceiver : BroadcastReceiver() {
    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var alarmMethods: AlarmMethods

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action == "android.intent.action.BOOT_COMPLETED") {
            alarmMethods.onBootAlarms(context!!)
        } else {
            val idTask = intent.getIntExtra(ID_TASK, 0)
            CoroutineScope(SupervisorJob()).launch {
                val receivedTask = taskDao.getTask(idTask).first()

                when (intent.getIntExtra(NOTIFICATION_CODE, 0)) {
                    0 -> {
                        showReminder(
                            context!!,
                            receivedTask
                        )
                    }
                    1 -> {
                        showReminderDeadline(
                            context!!,
                            receivedTask
                        )
                    }
                    2 -> {
                        showDeadline(
                            context!!,
                            receivedTask
                        )
                    }
                }
            }

        }

    }

    private fun showReminder(
        context: Context,
        task: TaskType
    ) {
        CoroutineScope(SupervisorJob()).launch {
            val notification: Boolean =
                context.dataStore.data.first()[PreferencesManager.PreferencesKeys.NOTIFICATIONS_MODE]
                    ?: true
            val reminders: Boolean =
                context.dataStore.data.first()[PreferencesManager.PreferencesKeys.REMINDERS_MODE]
                    ?: true
            if (notification && reminders) {
                ContextCompat.getSystemService(
                    context,
                    NotificationManager::
                    class.java
                )?.sendReminderNotification(
                    context,
                    context.resources.getString(R.string.reminder_msg),
                    task.idTask,
                    task.title,
                    task.created
                )
            }

            when (task.repeatMode) {
                0 -> {
                    taskDao.update(task.copy(time = GLOBAL_START_DATE))
                    alarmMethods.removeSharedPrefReminder(context, task.idTask)
                }
                1 -> {
                    val time = task.time + AlarmManager.INTERVAL_DAY
                    taskDao.update(task.copy(time = time))
                    alarmMethods.setReminder(context, task.idTask, task.repeatMode, time)
                }
                2 -> {
                    val time = task.time + (AlarmManager.INTERVAL_DAY * 7)
                    taskDao.update(task.copy(time = time))
                    alarmMethods.setReminder(context, task.idTask, task.repeatMode, time)
                }
            }
            //WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<CloudSyncWorker>().build())
        }
    }

    private fun showReminderDeadline(
        context: Context,
        task: TaskType,
    ) {
        CoroutineScope(SupervisorJob()).launch {
            val notification: Boolean =
                context.dataStore.data.first()[PreferencesManager.PreferencesKeys.NOTIFICATIONS_MODE]
                    ?: true
            if (notification) {
                ContextCompat.getSystemService(
                    context,
                    NotificationManager::
                    class.java
                )?.sendDeadlineNotification(
                    context,
                    context.resources.getString(R.string.deadline_reminder),
                    task.idTask,
                    task.title,
                    task.created
                )
            }
            ContextCompat.getSystemService(context, AlarmManager::class.java)?.setExact(
                AlarmManager.RTC_WAKEUP,
                task.deadline,
                PendingIntent.getBroadcast(
                    context,
                    "$UNIQUE_DEADLINE_ID${task.idTask}".toInt(),
                    Intent(context, AlarmServiceReceiver::class.java).apply {
                        putExtra(ID_TASK, task.idTask)
                        putExtra(NOTIFICATION_CODE, 2)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
    }

    private fun showDeadline(
        context: Context,
        task: TaskType,
    ) {
        CoroutineScope(SupervisorJob()).launch {
            if (task.checked) {
                return@launch
            } else {
                val notification: Boolean =
                    context.dataStore.data.first()[PreferencesManager.PreferencesKeys.NOTIFICATIONS_MODE]
                        ?: true
                if (notification) {
                    ContextCompat.getSystemService(
                        context,
                        NotificationManager::
                        class.java
                    )?.sendDeadlineNotification(
                        context,
                        context.resources.getString(R.string.deadline_msg),
                        task.idTask,
                        task.title,
                        task.created
                    )
                }
                taskDao.update(task.copy(missed = true))
                alarmMethods.removeSharedPrefDeadline(context, task.idTask)
            }
        }
    }
}

