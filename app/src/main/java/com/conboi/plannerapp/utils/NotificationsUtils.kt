package com.conboi.plannerapp.utils

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.conboi.plannerapp.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
fun NotificationManager.sendReminderNotification(
    context: Context,
    messageBody: String,
    idTask: Int,
    titleTask: String
) {
    if (!areNotificationsEnabled()) return
    val contentIntent = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.taskDetailsFragment, bundleOf(Pair(ID_TASK, idTask), Pair(NOTIFY_INTENT, true )))
        .createPendingIntent()

    val builder = NotificationCompat.Builder(
        context,
        context.getString(R.string.reminder_notification_channel_id)
    )
        .setSmallIcon(R.mipmap.ic_launcher_foreground)
        .setContentTitle(titleTask)
        .setContentText(messageBody)
        .setContentIntent(contentIntent)
        .setGroup(GROUP_NOTIFICATION_REMINDERS)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    val summaryNotification = NotificationCompat.Builder(
        context,
        context.resources.getString(R.string.reminder_notification_channel_id)
    )
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.resources.getString(R.string.check_tasks))
        .setSmallIcon(R.mipmap.ic_launcher_foreground)
        //build summary info into InboxStyle template
        .setStyle(
            NotificationCompat.InboxStyle()
                .setSummaryText(context.resources.getString(R.string.reminder_notification_channel_name))
        )
        .setAutoCancel(true)
        //specify which group this notification belongs to
        .setGroup(GROUP_NOTIFICATION_REMINDERS)
        .setGroupSummary(true)

    notify(idTask, builder.build())
    notify(-1, summaryNotification.build())
}

fun NotificationManager.sendDeadlineNotification(
    applicationContext: Context,
    messageBody: String,
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
        applicationContext.getString(R.string.deadline_notification_channel_id)
    )
        .setSmallIcon(R.mipmap.ic_launcher_foreground)
        .setContentTitle(titleTask)
        .setContentText(messageBody)
        .setContentIntent(contentIntent)
        .setGroup(GROUP_NOTIFICATION_DEADLINES)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    val summaryNotification = NotificationCompat.Builder(
        applicationContext,
        applicationContext.resources.getString(R.string.deadline_notification_channel_id)
    )
        .setSmallIcon(R.mipmap.ic_launcher_foreground)
        .setContentTitle(applicationContext.getString(R.string.app_name))
        .setContentText(applicationContext.resources.getString(R.string.check_deadlines))
        //build summary info into InboxStyle template
        .setStyle(
            NotificationCompat.InboxStyle()
                .setSummaryText(applicationContext.resources.getString(R.string.deadline_notification_channel_name))
        )
        .setAutoCancel(true)
        //specify which group this notification belongs to
        .setGroup(GROUP_NOTIFICATION_DEADLINES)
        .setGroupSummary(true)

    notify(idTask, builder.build())
    notify(-2, summaryNotification.build())
}



