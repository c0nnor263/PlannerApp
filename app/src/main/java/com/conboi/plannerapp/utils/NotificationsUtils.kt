package com.conboi.plannerapp.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.model.TaskType

const val NOTIFY_INTENT = "notifyIntent"

fun NotificationManager.sendReminderNotification(
    context: Context,
    messageBody: String,
    idTask: Int,
    titleTask: String,
    created: Long
) {
    if (!areNotificationsEnabled()) return
    val notificationChannelId =
        context.resources.getString(R.string.reminder_notification_channel_id)
    val notificationChannelName =
        context.resources.getString(R.string.reminder_notification_channel_name)

    val taskIntent = getActionPendingIntent(context, R.id.taskDetailsFragment, idTask)
    val mainIntent = getActionPendingIntent(context, R.id.mainFragment)
    val appIcon =
        context.packageManager.getApplicationIcon(context.packageName).let {
            drawableToBitmap(it)
        }
    val smallIcon = getSmallIcon(context, R.drawable.ic_app_icon)

    val builder = createBaseNotification(
        context,
        notificationChannelId = notificationChannelId,
        smallIcon = smallIcon,
        headIcon = appIcon,
        title = titleTask,
        messageBody = messageBody,
        intentAction = taskIntent,
        groupKey = NotificationGroup.GROUP_NOTIFICATION_REMINDER
    )

    val summaryNotification = createBaseSummaryNotification(
        context,
        notificationChannelId = notificationChannelId,
        contentTitle = context.getString(R.string.app_name),
        contextText = context.resources.getString(R.string.check_tasks),
        smallIcon = smallIcon,
        style = NotificationCompat.InboxStyle()
            .setSummaryText(notificationChannelName),
        intentAction = mainIntent,
        groupKey = NotificationGroup.GROUP_NOTIFICATION_REMINDER
    )

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
    val notificationChannelId = context.getString(R.string.deadline_notification_channel_id)
    val notificationChannelName =
        context.resources.getString(R.string.deadline_notification_channel_name)

    val taskIntent = getActionPendingIntent(context, R.id.taskDetailsFragment, idTask)
    val mainIntent = getActionPendingIntent(context, R.id.mainFragment)
    val appIcon =
        context.packageManager.getApplicationIcon(context.packageName).let {
            drawableToBitmap(it)
        }
    val smallIcon = getSmallIcon(context, R.drawable.ic_app_icon)

    val builder = createBaseNotification(
        context,
        notificationChannelId = notificationChannelId,
        smallIcon = smallIcon,
        headIcon = appIcon,
        title = titleTask,
        messageBody = messageBody,
        intentAction = taskIntent,
        groupKey = NotificationGroup.GROUP_NOTIFICATION_DEADLINE
    )


    val style = NotificationCompat.InboxStyle()
        .setSummaryText(notificationChannelName)

    val summaryNotification = createBaseSummaryNotification(
        context,
        notificationChannelId = notificationChannelId,
        smallIcon = smallIcon,
        contentTitle = context.getString(R.string.app_name),
        contextText = context.resources.getString(R.string.check_deadlines),
        style = style,
        intentAction = mainIntent,
        groupKey = NotificationGroup.GROUP_NOTIFICATION_DEADLINE
    )

    notify(idTask + created.toInt() - 2, builder.build())
    notify(-2, summaryNotification.build())
}

fun NotificationManager.sendNewFriendNotification(
    context: Context,
    messageBody: String,
    idNotify: Int
) {
    if (!areNotificationsEnabled()) return
    val notificationChannelId = context.getString(R.string.friends_notification_channel_id)

    val friendIntent = getActionPendingIntent(context, R.id.friendsFragment)
    val appIcon =
        context.packageManager.getApplicationIcon(context.packageName).let {
            drawableToBitmap(it)
        }
    val smallIcon = getSmallIcon(context, R.drawable.ic_baseline_person_add_24)

    val builder = createBaseNotification(
        context,
        notificationChannelId = notificationChannelId,
        smallIcon = smallIcon,
        headIcon = appIcon,
        title = context.resources.getString(R.string.new_friend),
        messageBody = messageBody,
        intentAction = friendIntent,
        groupKey = NotificationGroup.GROUP_NOTIFICATION_FRIEND
    )

    val style = NotificationCompat.InboxStyle()
        .setSummaryText(context.resources.getString(R.string.friends_notification_channel_name))

    val summaryNotification = createBaseSummaryNotification(
        context,
        notificationChannelId = notificationChannelId,
        contentTitle = context.getString(R.string.app_name),
        contextText = context.resources.getString(R.string.check_new_friends),
        smallIcon = smallIcon,
        style = style,
        intentAction = friendIntent,
        groupKey = NotificationGroup.GROUP_NOTIFICATION_FRIEND
    )

    notify(idNotify, builder.build())
    notify(-3, summaryNotification.build())
}


fun getActionPendingIntent(
    context: Context,
    @IdRes destinationId: Int,
    idTask: Int? = null
): PendingIntent {
    val bundle =
        when (idTask) {
            null -> {
                bundleOf(
                    Pair(NOTIFY_INTENT, true)
                )
            }
            else -> {
                bundleOf(
                    Pair(NOTIFY_INTENT, true),
                    Pair(TaskType.COLUMN_ID, idTask)
                )
            }
        }

    return NavDeepLinkBuilder(context)
        .setGraph(R.navigation.nav_graph)
        .setDestination(
            destinationId,
            bundle
        )
        .createPendingIntent()
}

fun getSmallIcon(context: Context, @DrawableRes iconId: Int) =
    IconCompat.createWithBitmap(
        drawableToBitmap(
            ContextCompat.getDrawable(
                context,
                iconId
            )!!
        )!!
    )

fun createBaseNotification(
    context: Context,
    notificationChannelId: String,
    smallIcon: IconCompat,
    headIcon: Bitmap?,
    title: String,
    messageBody: String,
    intentAction: PendingIntent,
    groupKey: NotificationGroup
) =
    NotificationCompat.Builder(
        context,
        notificationChannelId
    )
        .setSmallIcon(smallIcon)
        .setLargeIcon(headIcon)
        .setContentTitle(title)
        .setContentText(messageBody)
        .setContentIntent(intentAction)
        .setGroup(groupKey.name)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

fun createBaseSummaryNotification(
    context: Context,
    notificationChannelId: String,
    contentTitle: String,
    contextText: String,
    smallIcon: IconCompat,
    style: NotificationCompat.Style,
    intentAction: PendingIntent,
    groupKey: NotificationGroup
) =
    NotificationCompat.Builder(
        context,
        notificationChannelId
    )
        .setContentTitle(contentTitle)
        .setContentText(contextText)
        .setSmallIcon(
            smallIcon
        )
        .setStyle(
            style
        )
        .setContentIntent(intentAction)
        .setAutoCancel(true)
        .setGroup(groupKey.name)
        .setGroupSummary(true)

