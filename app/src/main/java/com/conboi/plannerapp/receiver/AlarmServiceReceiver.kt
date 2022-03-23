package com.conboi.plannerapp.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.dao.TaskDao
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.data.source.local.preferences.UserSettingsPreferencesDataStore
import com.conboi.plannerapp.di.AppApplicationScope
import com.conboi.plannerapp.di.IODispatcher
import com.conboi.plannerapp.utils.*
import com.conboi.plannerapp.utils.shared.AlarmUtil
import com.conboi.plannerapp.utils.shared.NOTIFICATION_CODE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class AlarmServiceReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    }

    @AppApplicationScope
    @Inject
    lateinit var applicationScope: CoroutineScope

    @IODispatcher
    @Inject
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var userSettingsPreferencesDataStore: UserSettingsPreferencesDataStore

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action == ACTION_BOOT_COMPLETED) {
            alarmUtil.onBootAlarms(context!!)
        } else {
            val idTask = intent.getIntExtra(TaskType.COLUMN_ID, 0)
            val notificationCode =
                NotificationType.valueOf(
                    intent.getStringExtra(NOTIFICATION_CODE) ?: NotificationType.REMINDER.name
                )

            applicationScope.launch {
                val task = taskDao.getTask(idTask).first()

                when (notificationCode) {
                    NotificationType.REMINDER -> showReminder(context!!, task)
                    NotificationType.REMINDER_FOR_DEADLINE -> {
                        showReminderForDeadline(context!!, task)
                    }
                    NotificationType.DEADLINE -> showDeadline(context!!, task)
                }
            }
        }
    }

    private fun showReminder(
        context: Context,
        task: TaskType
    ) {
        applicationScope.launch {
            withContext(ioDispatcher) {
                val isReminderEnabled = checkReminderSetting()
                val isNotificationEnabled = checkNotificationSetting()

                if (isNotificationEnabled && isReminderEnabled) {
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
                    RepeatMode.Once -> {
                        taskDao.update(task.copy(time = GLOBAL_START_DATE))
                        alarmUtil.removePrefReminder(task.idTask)
                    }
                    RepeatMode.Daily -> {
                        val dayTime = task.time + AlarmManager.INTERVAL_DAY
                        taskDao.update(task.copy(time = dayTime))
                        alarmUtil.setReminder(context, task.idTask, task.repeatMode, dayTime)
                    }
                    RepeatMode.Weekly -> {
                        val weekTime = task.time + (AlarmManager.INTERVAL_DAY * 7)
                        taskDao.update(task.copy(time = weekTime))
                        alarmUtil.setReminder(context, task.idTask, task.repeatMode, weekTime)
                    }
                }
            }
        }
    }

    private fun showReminderForDeadline(
        context: Context,
        task: TaskType,
    ) {
        applicationScope.launch {
            withContext(ioDispatcher) {
                if (checkNotificationSetting()) {
                    ContextCompat.getSystemService(
                        context,
                        NotificationManager::
                        class.java
                    )?.sendDeadlineNotification(
                        context,
                        context.resources.getString(
                            R.string.deadline_reminder,
                            DEADLINE_REMINDER_HOURS
                        ),
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
                        getUniqueRequestCode(AlarmType.DEADLINE, task.idTask),
                        Intent(context, AlarmServiceReceiver::class.java).apply {
                            putExtra(TaskType.COLUMN_ID, task.idTask)
                            putExtra(NOTIFICATION_CODE, NotificationType.DEADLINE)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }
    }

    private fun showDeadline(
        context: Context,
        task: TaskType,
    ) {
        applicationScope.launch {
            withContext(ioDispatcher) {
                if (task.checked) {
                    return@withContext
                } else {
                    if (checkNotificationSetting()) {
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
                    alarmUtil.removePrefDeadline(task.idTask)
                }
            }
        }
    }

    private suspend fun checkNotificationSetting(): Boolean =
        userSettingsPreferencesDataStore.preferencesFlow.first().notificationState

    private suspend fun checkReminderSetting(): Boolean =
        userSettingsPreferencesDataStore.preferencesFlow.first().reminderState

}

