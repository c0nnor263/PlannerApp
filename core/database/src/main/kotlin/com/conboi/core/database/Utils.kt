package com.conboi.core.database

import com.conboi.core.domain.enums.AlarmType

fun getTimeFromString(
    strTime: String?,
    alarmType: AlarmType
) = when (alarmType) {
    AlarmType.REMINDER -> strTime?.split(":")?.first() ?: "0"
    AlarmType.DEADLINE -> strTime?.split(":")?.last() ?: "0"
    AlarmType.ALL -> ""
}

fun getStringTimeWithReset(
    resetFor: AlarmType,
    strTime: String?
) = when (resetFor) {
    AlarmType.REMINDER -> "0:$strTime"
    AlarmType.DEADLINE -> "$strTime:0"
    AlarmType.ALL -> ""
}