package com.conboi.plannerapp.utils

enum class InsetSide { LEFT, TOP, RIGHT, BOTTOM }
enum class BottomAction { DELETE, SORT }

enum class SortOrder { BY_TITLE, BY_DATE, BY_COMPLETE, BY_OVERCOMPLETED }
enum class SynchronizationState { PENDING_SYNC, COMPLETE_SYNC, ERROR_SYNC, DISABLED_SYNC }
enum class RepeatMode { Once, Daily, Weekly }
enum class Priority { LEISURELY, DEFAULT, ADVISABLE, IMPORTANT }

enum class AlarmType { REMINDER, DEADLINE, ALL }
enum class PremiumType { STANDARD, MONTH, SIX_MONTH, YEAR }
enum class NotificationType { REMINDER, REMINDER_FOR_DEADLINE, DEADLINE }
enum class NotificationGroup {
    GROUP_NOTIFICATION_REMINDER,
    GROUP_NOTIFICATION_DEADLINE,
    GROUP_NOTIFICATION_FRIEND
}

enum class InviteFriendError { ADD_YOURSELF, FRIEND_ALREADY, NOT_EXIST, }
enum class InsertMultipleTaskError { MAXIMUM, INCORRECT }


