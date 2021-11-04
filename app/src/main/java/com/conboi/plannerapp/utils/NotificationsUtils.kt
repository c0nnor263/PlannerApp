package com.conboi.plannerapp.utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.conboi.plannerapp.R


const val REMINDERS = "remindersFile"
const val REMINDER_ID_SET = "reminderIdsSet"
const val ID_TASK = "idTask"
const val TITLE_TASK = "titleTask"
const val TRIGGER_TASK = "triggerTask"
const val REPEAT_MODE_TASK = "repeatMode"
const val GROUP_KEY_REMINDERS = "groupReminders"

fun NotificationManager.sendReminderNotification(
    messageBody: String,
    applicationContext: Context,
    idTask: Int,
    titleTask: String
) {
    if (!areNotificationsEnabled()) return
    val contentIntent = NavDeepLinkBuilder(applicationContext)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.taskDetailsFragment)
        .setArguments(bundleOf(Pair(ID_TASK, idTask)))
        .createPendingIntent()

    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.reminder_notification_channel_id)
    )
        .setSmallIcon(R.mipmap.ic_launcher_foreground)
        .setContentTitle(titleTask)
        .setContentText(messageBody)
        .setContentIntent(contentIntent)
        .setGroup(GROUP_KEY_REMINDERS)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    val summaryNotification = NotificationCompat.Builder(
        applicationContext,
        applicationContext.resources.getString(R.string.reminder_notification_channel_id)
    )
        .setContentTitle(applicationContext.getString(R.string.app_name))
        .setContentText(applicationContext.resources.getString(R.string.check_tasks))
        .setSmallIcon(R.mipmap.ic_launcher)
        //build summary info into InboxStyle template
        .setStyle(
            NotificationCompat.InboxStyle()
                .setSummaryText(applicationContext.resources.getString(R.string.reminder_notification_channel_name))
        )
        .setAutoCancel(true)
        //specify which group this notification belongs to
        .setGroup(GROUP_KEY_REMINDERS)
        .setGroupSummary(true)

    notify(idTask, builder.build())
    notify(-1, summaryNotification.build())
}

fun NotificationManager.cancelAllNotifications(applicationContext: Context) {
    val sharedPref = applicationContext.getSharedPreferences(
        REMINDERS, Context.MODE_PRIVATE
    ) ?: return
    with(sharedPref) {
        edit().apply {
            clear()
        }
    }
    cancelAll()
}

fun AlarmManager.setOrUpdateReminder(
    applicationContext: Context,
    id: Int,
    title: String,
    triggerTime: Long,
    repeatMode: Int
) {
    val sharedPref = applicationContext.getSharedPreferences(
        REMINDERS, Context.MODE_PRIVATE
    ) ?: return

    /*val pendingIntent = Intent(applicationContext, AlarmReceiver::class.java).let { intent ->
        intent.putExtra(ID_TASK, id)
        intent.putExtra(TITLE_TASK, title)
        PendingIntent.getBroadcast(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    when (repeatMode) {
        0 -> {
            //One shot
            set(
                RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
        1 -> {
            //Day
            setInexactRepeating(
                RTC_WAKEUP,
                triggerTime,
                INTERVAL_DAY,
                pendingIntent
            )
        }
        2 -> {
            //Week
            setInexactRepeating(
                RTC_WAKEUP,
                triggerTime,
                INTERVAL_DAY * 7,
                pendingIntent
            )
        }

    }

    with(sharedPref) {
        val reminderSet =
            getStringSet(REMINDER_ID_SET, HashSet<String>(hashSetOf(id.toString())))
        val newReminderSet = HashSet<String>(hashSetOf(id.toString()))
        newReminderSet.addAll(reminderSet!!)

        edit().apply {
            putStringSet(REMINDER_ID_SET, newReminderSet)
            putString("$TITLE_TASK$id", title)
            putLong("$TRIGGER_TASK$id", triggerTime)
            putInt("$REPEAT_MODE_TASK$id", repeatMode)
            apply()
        }
    }*/
}

fun AlarmManager.cancelReminder(
    applicationContext: Context, id: Int
) {
    val sharedPref = applicationContext.getSharedPreferences(
        REMINDERS, Context.MODE_PRIVATE
    ) ?: return

   // val intent = Intent(applicationContext, AlarmReceiver::class.java)

    /*val pendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        id,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
    )
    cancel(pendingIntent)*/
    with(sharedPref) {
        val newReminderSet = HashSet<String>(hashSetOf())
        val reminderSet = getStringSet(REMINDER_ID_SET, newReminderSet)
        newReminderSet.addAll(reminderSet!!)
        newReminderSet.remove(id.toString())
        edit().apply {
            putStringSet(REMINDER_ID_SET, newReminderSet)
            remove("$TITLE_TASK$id")
            remove("$TRIGGER_TASK$id")
            remove("$REPEAT_MODE_TASK$id")
            apply()
        }
    }
}

fun AlarmManager.onBootReminders(applicationContext: Context) {
    val sharedPref = applicationContext.getSharedPreferences(
        REMINDERS, Context.MODE_PRIVATE
    ) ?: return
    with(sharedPref) {
        val reminderSet = getStringSet(REMINDER_ID_SET, null)
        for (id in reminderSet!!) {
            setOrUpdateReminder(
                applicationContext,
                id.toInt(),
                getString("$TITLE_TASK${id.toInt()}}", "").toString(),
                getLong("$TRIGGER_TASK${id.toInt()}", 0),
                getInt("$REPEAT_MODE_TASK${id.toInt()}", 0)
            )
        }
    }
}
