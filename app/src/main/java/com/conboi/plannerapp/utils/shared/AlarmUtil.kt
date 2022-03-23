package com.conboi.plannerapp.utils.shared

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.conboi.plannerapp.data.dao.TaskDao
import com.conboi.plannerapp.data.model.TaskType
import com.conboi.plannerapp.data.source.local.preferences.ALARM_IS_NOT_EMPTY
import com.conboi.plannerapp.data.source.local.preferences.AlarmPreferencesDataStore
import com.conboi.plannerapp.di.AppApplicationScope
import com.conboi.plannerapp.di.IODispatcher
import com.conboi.plannerapp.receiver.AlarmServiceReceiver
import com.conboi.plannerapp.utils.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

const val NOTIFICATION_CODE = "NotifyCode"

@Singleton
class AlarmUtil @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmPreferencesDataStore: AlarmPreferencesDataStore,
    @AppApplicationScope private val applicationScope: CoroutineScope,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    fun setReminder(
        context: Context,
        idTask: Int,
        repeatMode: RepeatMode,
        reminderTime: Long
    ) {
        var time = reminderTime

        if (reminderTime < System.currentTimeMillis()) {
            val oldTime = Calendar.getInstance().apply {
                timeInMillis = reminderTime
            }
            val currentTime = Calendar.getInstance()

            val oldDayWeek = oldTime[Calendar.DAY_OF_WEEK]
            val oldHour = oldTime[Calendar.HOUR_OF_DAY]

            val currentDayWeek = currentTime[Calendar.DAY_OF_WEEK]
            val currentHour = currentTime[Calendar.HOUR_OF_DAY]


            when (repeatMode) {
                RepeatMode.Once -> return
                RepeatMode.Daily -> {
                    if (oldHour < currentHour) {
                        currentTime.add(Calendar.DATE, 1)
                    }
                }
                RepeatMode.Weekly -> {
                    if (oldDayWeek >= currentDayWeek) {
                        currentTime.add(Calendar.DATE, oldDayWeek - currentDayWeek)
                    } else {
                        currentTime.add(Calendar.DATE, 7 - currentDayWeek)
                    }
                }
            }
            currentTime[Calendar.HOUR_OF_DAY] = oldTime[Calendar.HOUR_OF_DAY]
            currentTime[Calendar.MINUTE] = oldTime[Calendar.MINUTE]
            time = currentTime.timeInMillis
        }

        val pendingIntent = Intent(context, AlarmServiceReceiver::class.java).let {
            PendingIntent.getBroadcast(
                context,
                getUniqueRequestCode(AlarmType.REMINDER, idTask),
                it.apply {
                    putExtra(TaskType.COLUMN_ID, idTask)
                    putExtra(NOTIFICATION_CODE, NotificationType.REMINDER)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager?.setExact(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )

        addPrefReminder(idTask, time)
        enableBroadcastReceiver(context)
    }

    fun setDeadline(
        context: Context,
        idTask: Int,
        deadlineTime: Long,
    ) {
        val intent = Intent(context, AlarmServiceReceiver::class.java).apply {
            putExtra(TaskType.COLUMN_ID, idTask)
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        if (deadlineTime - TimeUnit.HOURS.toMillis(24) > System.currentTimeMillis()) {
            alarmManager?.setExact(
                AlarmManager.RTC_WAKEUP,
                deadlineTime - TimeUnit.HOURS.toMillis(24),
                PendingIntent.getBroadcast(
                    context,
                    getUniqueRequestCode(AlarmType.DEADLINE, idTask),
                    intent.apply {
                        putExtra(NOTIFICATION_CODE, NotificationType.REMINDER_FOR_DEADLINE)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            alarmManager?.setExact(
                AlarmManager.RTC_WAKEUP, deadlineTime, PendingIntent.getBroadcast(
                    context,
                    getUniqueRequestCode(AlarmType.DEADLINE, idTask),
                    intent.putExtra(NOTIFICATION_CODE, NotificationType.DEADLINE),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        addPrefDeadline(idTask, deadlineTime)
        enableBroadcastReceiver(context)
    }

    fun cancelReminder(
        context: Context,
        idTask: Int
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = Intent(context, AlarmServiceReceiver::class.java).let {
            PendingIntent.getBroadcast(
                context,
                getUniqueRequestCode(AlarmType.REMINDER, idTask),
                it,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        }
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            ContextCompat.getSystemService(context, NotificationManager::class.java)?.cancel(idTask)

            removePrefReminder(idTask)

            applicationScope.launch {
                withContext(ioDispatcher) {
                    try {
                        val task = taskDao.getTask(idTask).first()
                        taskDao.update(
                            task.copy(
                                time = GLOBAL_START_DATE,
                                repeatMode = RepeatMode.Once
                            )
                        )
                        checkAlarmFileCounts(context)
                    } catch (e: Exception) {
                        Log.d("TAG", "cancelReminder: $e")
                    }
                }
            }
        }
    }

    fun cancelDeadline(
        context: Context,
        idTask: Int
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = Intent(context, AlarmServiceReceiver::class.java).let {
            PendingIntent.getBroadcast(
                context,
                getUniqueRequestCode(AlarmType.DEADLINE, idTask),
                it,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        }
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            ContextCompat.getSystemService(context, NotificationManager::class.java)?.cancel(idTask)

            removePrefDeadline(idTask)

            applicationScope.launch {
                withContext(ioDispatcher) {
                    try {
                        val task = taskDao.getTask(idTask).first()
                        taskDao.update(task.copy(deadline = GLOBAL_START_DATE, missed = false))
                        checkAlarmFileCounts(context)
                    } catch (e: Exception) {
                        Log.d("TAG", "cancelDeadline: $e")
                    }
                }
            }
        }
    }


    private fun addPrefReminder(id: Int, time: Long) =
        applicationScope.launch {
            withContext(ioDispatcher) {
                alarmPreferencesDataStore.addReminderPref(id, time)
            }
        }

    private fun addPrefDeadline(id: Int, time: Long) =
        applicationScope.launch {
            withContext(ioDispatcher) {
                alarmPreferencesDataStore.addDeadlinePref(id, time)
            }
        }


    fun removePrefReminder(id: Int) =
        applicationScope.launch {
            withContext(ioDispatcher) {
                alarmPreferencesDataStore.removeReminderPref(id)
            }
        }

    fun removePrefDeadline(id: Int) =
        applicationScope.launch {
            withContext(ioDispatcher) {
                alarmPreferencesDataStore.removeDeadlinePref(id)
            }
        }


    private suspend fun checkAlarmFileCounts(context: Context) {
        val alarmPreferencesSize = alarmPreferencesDataStore.preferencesFlow.first().asMap().size
        if (alarmPreferencesSize <= 1) {
            disableBroadcastReceiver(context)
        } else {
            enableBroadcastReceiver(context)
        }
    }

    fun cancelAllAlarmsType(context: Context, alarmType: AlarmType) {
        applicationScope.launch {
            withContext(ioDispatcher) {
                val alarmFile =
                    alarmPreferencesDataStore.preferencesFlow.first().asMap().toMutableMap()
                alarmFile.remove(booleanPreferencesKey(ALARM_IS_NOT_EMPTY))

                when (alarmType) {
                    AlarmType.ALL -> {
                        for (alarmId in alarmFile) {
                            cancelReminder(context, (alarmId.key.name).toInt())
                            cancelDeadline(context, (alarmId.key.name).toInt())
                        }
                    }
                    AlarmType.REMINDER -> {
                        //All reminders cancel
                        for (alarmId in alarmFile) {
                            cancelReminder(context, (alarmId.key.name).toInt())
                        }
                    }
                    AlarmType.DEADLINE -> {
                        //All deadlines cancel
                        for (alarmId in alarmFile) {
                            cancelDeadline(context, (alarmId.key.name).toInt())
                        }
                    }
                }
                alarmPreferencesDataStore.clear()
            }
        }
        disableBroadcastReceiver(context)
    }


    fun onBootAlarms(context: Context) {
        applicationScope.launch {
            val alarmFile =
                alarmPreferencesDataStore.preferencesFlow.first().asMap().toMutableMap().apply {
                    if (this.size <= 1) {
                        disableBroadcastReceiver(context)
                        return@launch
                    }
                    remove(booleanPreferencesKey(ALARM_IS_NOT_EMPTY))
                } as MutableMap<Preferences.Key<String>, String>
            for ((id) in alarmFile) {

                val reminderTime = getTimeFromString(alarmFile[id], AlarmType.REMINDER)
                val deadlineTime = getTimeFromString(alarmFile[id], AlarmType.DEADLINE)
                if (reminderTime != GLOBAL_START_DATE.toString() && reminderTime.isNotBlank()) {
                    setReminder(
                        context,
                        id.name.toInt(),
                        RepeatMode.Once,
                        reminderTime.toLong()
                    )
                }
                if (deadlineTime != GLOBAL_START_DATE.toString() && deadlineTime.isNotBlank()) {
                    setDeadline(
                        context,
                        id.name.toInt(),
                        deadlineTime.toLong()
                    )
                }
            }
        }
    }


    private fun enableBroadcastReceiver(context: Context) {
        val receiver = ComponentName(context, AlarmServiceReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun disableBroadcastReceiver(context: Context) {
        val receiver = ComponentName(context, AlarmServiceReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}