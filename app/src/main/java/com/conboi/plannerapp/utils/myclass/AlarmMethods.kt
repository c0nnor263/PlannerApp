package com.conboi.plannerapp.utils.myclass

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        val prefs = context.getSharedPreferences(ALARM_FILE, Context.MODE_PRIVATE) ?: return
        if (time < System.currentTimeMillis()) {
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
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager?.setExact(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )


        prefs.apply {
            val oldValue = getString(idTask.toString(), "")?.split(":") ?: return
            edit().apply {
                putBoolean(ALARM_FILE_INITIALIZED, true)
                putString(
                    idTask.toString(), "$time:${oldValue.last()}"
                )
                apply()
            }
        }
        val receiver = ComponentName(context, AlarmServiceReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun setDeadline(
        context: Context,
        idTask: Int,
        time: Long,
    ) {
        val prefs = context.getSharedPreferences(ALARM_FILE, Context.MODE_PRIVATE) ?: return
        val intent = Intent(context, AlarmServiceReceiver::class.java).apply {
            putExtra(ID_TASK, idTask)
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        if (time - TimeUnit.HOURS.toMillis(24) > System.currentTimeMillis()) {
            alarmManager?.setExact(
                AlarmManager.RTC_WAKEUP,
                time - TimeUnit.HOURS.toMillis(24),
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
            val oldValue = getString(idTask.toString(), "")?.split(":") ?: return
            edit().apply {
                putBoolean(ALARM_FILE_INITIALIZED, true)
                putString(
                    idTask.toString(), "${oldValue.first()}:$time"
                )
                apply()
            }
        }
        val receiver = ComponentName(context, AlarmServiceReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun cancelReminder(
        context: Context,
        idTask: Int
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
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

            removeSharedPrefReminder(context, idTask)

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
        val alarmManager = context.getSystemService(AlarmManager::class.java)
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

            removeSharedPrefDeadline(context, idTask)

            CoroutineScope(SupervisorJob()).launch {
                try {
                    val task = taskDao.getTask(idTask).first()
                    taskDao.update(task.copy(deadline = GLOBAL_START_DATE, missed = false))
                } catch (e: Exception) {
                    Log.d("TAG", "cancelDeadline: $e")
                }
            }
        }
    }

    fun removeSharedPrefReminder(context: Context, idTask: Int) {
        val prefs = context.getSharedPreferences(ALARM_FILE, Context.MODE_PRIVATE) ?: return
        prefs.apply {
            val deadlineTime = getString(idTask.toString(), "")?.split(":")?.last() ?: "0"
            edit().apply {
                if (deadlineTime == GLOBAL_START_DATE.toString() || deadlineTime.isBlank()) {
                    remove(idTask.toString())
                    putBoolean(ALARM_FILE_INITIALIZED, false)
                    apply()
                } else {
                    putString(idTask.toString(), "0:${deadlineTime}")
                    apply()
                }
            }
        }
        checkAlarmFileCounts(context)
    }

    fun removeSharedPrefDeadline(context: Context, idTask: Int) {
        val prefs = context.getSharedPreferences(ALARM_FILE, Context.MODE_PRIVATE) ?: return
        prefs.apply {
            val reminderTime = getString(idTask.toString(), "")?.split(":")?.first() ?: "0"
            edit().apply {
                if (reminderTime == GLOBAL_START_DATE.toString()|| reminderTime.isBlank()) {
                    remove(idTask.toString())
                    putBoolean(ALARM_FILE_INITIALIZED, false)
                    apply()
                } else {
                    putString(idTask.toString(), "${reminderTime}:0")
                    apply()
                }
            }
        }
        checkAlarmFileCounts(context)
    }

    private fun checkAlarmFileCounts(context: Context) {
        val prefs = context.getSharedPreferences(ALARM_FILE, Context.MODE_PRIVATE) ?: return
        val receiver = ComponentName(context, AlarmServiceReceiver::class.java)
        prefs.apply {
            if (all.size <= 1) {
                context.packageManager.setComponentEnabledSetting(
                    receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                context.packageManager.setComponentEnabledSetting(
                    receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }

    }

    fun cancelAllAlarmsType(context: Context, remindersOrDeadlines: Boolean?) {
        val alarmFile = context.getSharedPreferences(
            ALARM_FILE,
            Context.MODE_PRIVATE
        ).all.apply {
            if (isNotEmpty()) {
                remove(ALARM_FILE_INITIALIZED)
            } else {
                return
            }
        } as MutableMap<String, String>


        when (remindersOrDeadlines) {
            null -> {
                for ((alarmId) in alarmFile) {
                    cancelReminder(context, alarmId.toInt())
                    cancelDeadline(context, alarmId.toInt())
                }
            }
            true -> {
                //All reminders cancel
                for ((alarmId) in alarmFile) {
                    cancelReminder(context, alarmId.toInt())
                }
            }
            false -> {
                //All deadlines cancel
                for ((alarmId) in alarmFile) {
                    cancelDeadline(context, alarmId.toInt())
                }
            }
        }
        context.getSharedPreferences(ALARM_FILE, Context.MODE_PRIVATE).edit()
            .apply {
                clear()
                putBoolean(ALARM_FILE_INITIALIZED, false)
                apply()
            }

        val receiver = ComponentName(context, AlarmServiceReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }


    fun onBootAlarms(context: Context) {
        val alarmFile = context.getSharedPreferences(
            ALARM_FILE,
            Context.MODE_PRIVATE
        ).all.apply {
            if (this.size <= 1) {
                context.packageManager.setComponentEnabledSetting(
                    ComponentName(context, AlarmServiceReceiver::class.java),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                return
            }
            remove(ALARM_FILE_INITIALIZED)
        } as MutableMap<String, String>

        for ((id) in alarmFile) {
            val reminderTime = (alarmFile[id]!!.split(":")).first()
            val deadlineTime = (alarmFile[id]!!.split(":")).last()

            if (reminderTime != GLOBAL_START_DATE.toString() && reminderTime.isNotBlank()) {
                setReminder(context, id.toInt(), 0, reminderTime.toLong())
            }
            if (deadlineTime != GLOBAL_START_DATE.toString() && deadlineTime.isNotBlank()) {
                setDeadline(context, id.toInt(), deadlineTime.toLong())
            }
        }
    }
}