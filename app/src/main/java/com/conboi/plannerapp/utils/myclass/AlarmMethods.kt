package com.conboi.plannerapp.utils.myclass

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.conboi.plannerapp.data.TaskDao
import com.conboi.plannerapp.receiver.AlarmServiceReceiver
import com.conboi.plannerapp.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AlarmMethods @Inject constructor(val taskDao: TaskDao) : Fragment() {
    fun setReminder(
        context: Context,
        idTask: Int,
        repeatMode: Int,
        inTime: Long
    ) {
        var time = inTime
        val prefs = context.getSharedPreferences(ALARMS_FILE, Context.MODE_PRIVATE) ?: return
        if (time <= System.currentTimeMillis()) {
            val oldTime = Calendar.getInstance().apply {
                timeInMillis = inTime
            }
            val currentTime = Calendar.getInstance()

            val oldDayWeek = oldTime[Calendar.DAY_OF_WEEK]
            val currentDayWeek = currentTime[Calendar.DAY_OF_WEEK]
            val oldHour = oldTime[Calendar.HOUR_OF_DAY]
            val currentHour = currentTime[Calendar.HOUR_OF_DAY]
            when (repeatMode) {
                0 -> return
                1 -> {
                    if (oldHour < currentHour) {
                        currentTime.add(Calendar.DATE, 1)
                    }
                }
                2 -> {
                    Log.d("TAG", "setReminder: $oldDayWeek $currentDayWeek ")
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
                "$UNIQUE_REMINDER_ID$idTask".toInt(),
                it.apply {
                    putExtra(ID_TASK, idTask)
                    putExtra(NOTIFICATION_CODE, 0)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        alarmManager?.setExact(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )


        prefs.apply {
            edit().apply {
                putBoolean(ALARMS_FILE_INITIALIZED, true)
                putString("$UNIQUE_REMINDER_ID$idTask", idTask.toString())
                apply()
            }
        }
    }

    fun setDeadline(
        context: Context,
        idTask: Int,
        time: Long,
    ) {
        val prefs = context.getSharedPreferences(ALARMS_FILE, Context.MODE_PRIVATE) ?: return
        val intent = Intent(context, AlarmServiceReceiver::class.java).apply {
            putExtra(ID_TASK, idTask)
        }

        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)

        if (time - TimeUnit.HOURS.toMillis(4) > System.currentTimeMillis()) {
            alarmManager?.setExact(
                AlarmManager.RTC_WAKEUP,
                time - TimeUnit.HOURS.toMillis(4),
                PendingIntent.getBroadcast(
                    context,
                    "$UNIQUE_DEADLINE_ID$idTask".toInt(),
                    intent.apply {
                        putExtra(NOTIFICATION_CODE, 1)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            alarmManager?.setExact(
                AlarmManager.RTC_WAKEUP, time, PendingIntent.getBroadcast(
                    context,
                    "$UNIQUE_DEADLINE_ID$idTask".toInt(),
                    intent.putExtra(NOTIFICATION_CODE, 2),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        prefs.apply {
            edit().apply {
                putBoolean(ALARMS_FILE_INITIALIZED, true)
                putString("$UNIQUE_DEADLINE_ID$idTask", idTask.toString())
                apply()
            }
        }
    }

    fun cancelReminder(
        context: Context,
        idTask: Int
    ) {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        val pendingIntent = Intent(context, AlarmServiceReceiver::class.java).let {
            PendingIntent.getBroadcast(
                context,
                "$UNIQUE_REMINDER_ID$idTask".toInt(),
                it,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        }
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            ContextCompat.getSystemService(context, NotificationManager::class.java)?.cancel(idTask)
            context.getSharedPreferences(ALARMS_FILE, Context.MODE_PRIVATE).edit()
                .remove("$UNIQUE_REMINDER_ID$idTask").apply()

            CoroutineScope(SupervisorJob()).launch {
                try {
                    val task = taskDao.getTask(idTask).first()
                    taskDao.update(task.copy(time = GLOBAL_START_DATE, repeatMode = 0))
                } catch (e: Exception) {
                    Log.d("TAG", "cancelReminder: $e")
                }
            }
        }
    }

    fun cancelDeadline(
        context: Context,
        idTask: Int
    ) {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        val pendingIntent = Intent(context, AlarmServiceReceiver::class.java).let {
            PendingIntent.getBroadcast(
                context,
                "$UNIQUE_DEADLINE_ID$idTask".toInt(),
                it,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        }
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            ContextCompat.getSystemService(context, NotificationManager::class.java)?.cancel(idTask)
            context.getSharedPreferences(ALARMS_FILE, Context.MODE_PRIVATE).edit()
                .remove("$UNIQUE_DEADLINE_ID$idTask").apply()
            CoroutineScope(SupervisorJob()).launch {
                try {
                    val task = taskDao.getTask(idTask).first()
                    taskDao.update(task.copy(deadline = GLOBAL_START_DATE))
                } catch (e: Exception) {
                    Log.d("TAG", "cancelReminder: $e")
                }
            }
        }
    }

    fun cancelAllAlarmsType(context: Context, remindersOrDeadlines: Boolean?) {
        val tasksAlarms = (context.getSharedPreferences(
            ALARMS_FILE,
            Context.MODE_PRIVATE
        ).all.values.toMutableList() as MutableList<String>)
        if (tasksAlarms.isNullOrEmpty()) {
            return
        }
        tasksAlarms.removeAt(0)
        val allTasksAlarms = tasksAlarms.map { idTask -> idTask.toInt() }
        when (remindersOrDeadlines) {
            null -> {
                for (alarmId in allTasksAlarms) {
                    cancelReminder(context, alarmId)
                    cancelDeadline(context, alarmId)
                }
            }
            true -> {
                //All reminders cancel
                for (alarmId in allTasksAlarms) {
                    cancelReminder(context, alarmId)
                }
            }
            false -> {
                //All deadlines cancel
                for (alarmId in allTasksAlarms) {
                    cancelDeadline(context, alarmId)
                }
            }
        }
        context.getSharedPreferences(ALARMS_FILE, Context.MODE_PRIVATE).edit()
            .apply {
                clear()
                putBoolean(ALARMS_FILE_INITIALIZED, false)
                apply()
            }
    }
}