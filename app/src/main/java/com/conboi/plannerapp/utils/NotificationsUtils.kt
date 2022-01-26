package com.conboi.plannerapp.utils

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.conboi.plannerapp.R
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
fun NotificationManager.sendReminderNotification(
    context: Context,
    messageBody: String,
    idTask: Int,
    titleTask: String,
    created: Long
) {
    if (!areNotificationsEnabled()) return
    val appIcon =
        context.packageManager.getApplicationIcon(context.packageName).let {
            drawableToBitmap(it)
        }
    val smallIcon = IconCompat.createWithBitmap(
        drawableToBitmap(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_app_icon
            )!!
        )
    )

    val taskIntent = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination(
            R.id.taskDetailsFragment,
            bundleOf(Pair(ID_TASK, idTask), Pair(NOTIFY_INTENT, true))
        )
        .createPendingIntent()

    val mainIntent = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination(
            (R.id.mainFragment),
            bundleOf(Pair(NOTIFY_INTENT, true))
        )
        .createPendingIntent()


    val builder = NotificationCompat.Builder(
        context,
        context.getString(R.string.reminder_notification_channel_id)
    )
        .setSmallIcon(smallIcon)
        .setLargeIcon(appIcon)
        .setContentTitle(titleTask)
        .setContentText(messageBody)
        .setContentIntent(taskIntent)
        .setGroup(GROUP_NOTIFICATION_REMINDERS)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    val summaryNotification = NotificationCompat.Builder(
        context,
        context.resources.getString(R.string.reminder_notification_channel_id)
    )
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.resources.getString(R.string.check_tasks))
        .setSmallIcon(
            smallIcon
        )
        //build summary info into InboxStyle template
        .setStyle(
            NotificationCompat.InboxStyle()
                .setSummaryText(context.resources.getString(R.string.reminder_notification_channel_name))
        )
        .setContentIntent(mainIntent)
        .setAutoCancel(true)
        .setGroup(GROUP_NOTIFICATION_REMINDERS)
        .setGroupSummary(true)

    notify(idTask + created.toInt() - 1, builder.build())
    notify(-1, summaryNotification.build())
}

fun NotificationManager.sendDeadlineNotification(
    context: Context,
    messageBody: String,
    idTask: Int,
    titleTask: String,
    created: Long
) {
    if (!areNotificationsEnabled()) return
    val appIcon =
        context.packageManager.getApplicationIcon(context.packageName).let {
            drawableToBitmap(it)
        }
    val smallIcon = IconCompat.createWithBitmap(
        drawableToBitmap(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_app_icon
            )!!
        )
    )

    val taskIntent = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.taskDetailsFragment)
        .setArguments(bundleOf(Pair(ID_TASK, idTask), Pair(NOTIFY_INTENT, true)))
        .createPendingIntent()

    val mainIntent = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination(
            (R.id.mainFragment),
            bundleOf(Pair(NOTIFY_INTENT, true))
        )
        .createPendingIntent()

    val builder = NotificationCompat.Builder(
        context,
        context.getString(R.string.deadline_notification_channel_id)
    )
        .setSmallIcon(smallIcon)
        .setLargeIcon(appIcon)
        .setContentTitle(titleTask)
        .setContentText(messageBody)
        .setContentIntent(taskIntent)
        .setGroup(GROUP_NOTIFICATION_DEADLINES)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    val summaryNotification = NotificationCompat.Builder(
        context,
        context.resources.getString(R.string.deadline_notification_channel_id)
    )
        .setSmallIcon(
            smallIcon
        )
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.resources.getString(R.string.check_deadlines))
        //build summary info into InboxStyle template
        .setStyle(
            NotificationCompat.InboxStyle()
                .setSummaryText(context.resources.getString(R.string.deadline_notification_channel_name))
        )
        .setContentIntent(mainIntent)
        .setAutoCancel(true)
        .setGroup(GROUP_NOTIFICATION_DEADLINES)
        .setGroupSummary(true)

    notify(idTask + created.toInt() - 2, builder.build())
    notify(-2, summaryNotification.build())
}

fun NotificationManager.sendNewFriendNotification(
    context: Context,
    messageBody: String,
    idNotify: Int
) {
    if (!areNotificationsEnabled()) return
    val appIcon =
        context.packageManager.getApplicationIcon(context.packageName).let {
            drawableToBitmap(it)
        }
    val smallIcon = IconCompat.createWithBitmap(
        drawableToBitmap(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_baseline_person_add_24
            )!!
        )
    )

    val taskIntent = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.friendsFragment)
        .setArguments(bundleOf(Pair(NOTIFY_INTENT, true)))
        .createPendingIntent()

    val friendIntent = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination(
            (R.id.friendsFragment),
            bundleOf(Pair(NOTIFY_INTENT, true))
        )
        .createPendingIntent()

    val builder = NotificationCompat.Builder(
        context,
        context.getString(R.string.deadline_notification_channel_id)
    )
        .setSmallIcon(smallIcon)
        .setLargeIcon(appIcon)
        .setContentTitle(context.resources.getString(R.string.new_friend))
        .setContentText(messageBody)
        .setContentIntent(taskIntent)
        .setGroup(GROUP_NOTIFICATION_FRIENDS)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    val summaryNotification = NotificationCompat.Builder(
        context,
        context.resources.getString(R.string.friends_notification_channel_id)
    )
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.resources.getString(R.string.check_new_friends))
        .setSmallIcon(
            smallIcon
        )
        .setStyle(
            NotificationCompat.InboxStyle()
                .setSummaryText(context.resources.getString(R.string.friends_notification_channel_name))
        )
        .setContentIntent(friendIntent)
        .setAutoCancel(true)
        .setGroup(GROUP_NOTIFICATION_FRIENDS)
        .setGroupSummary(true)

    notify(idNotify, builder.build())
    notify(-3, summaryNotification.build())
}



