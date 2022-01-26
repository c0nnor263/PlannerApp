package com.conboi.plannerapp.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.annotation.AttrRes
import androidx.core.content.res.use
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
const val PROJECT_KEY = "ofRBlNjdwHFUFAYItO9sEVKOdaXcN0Aw"


const val MAX_TASK_COUNT: Int = 50
const val MAX_ADD_TASK: Int = 15
const val MIDDLE_COUNT:Int = 50

const val GLOBAL_START_DATE: Long = 0L
const val LANGUAGE ="LANGUAGE"
const val APP_FILE = "APP_FILE"
const val FIRST_LAUNCH = "FIRST_LAUNCH"
const val IMPORT_CONFIRM = "IMPORT_CONFIRM"
const val IMPORT_DOWNLOADED = "IMPORT_DOWNLOADED"
const val EMAIL_CONFIRM = "EMAIL_CONFIRM"
const val RESUBSCRIBE_ALERT = "RESUBSCRIBE_ALERT"


const val ID_TASK = "idTask"

const val NOTIFY_INTENT = "notifyIntent"

const val ALARM_FILE = "alarmFile"
const val ALARM_FILE_INITIALIZED = "alarmFileInitialized"
const val UNIQUE_REMINDER_ID = "1111"
const val UNIQUE_DEADLINE_ID = "2222"


/*
0 - Reminder
1 - ReminderDeadline
2 - Deadline
*/
const val NOTIFICATION_CODE = "notifyCode"

const val GROUP_NOTIFICATION_REMINDERS = "groupNotificationReminders"
const val GROUP_NOTIFICATION_DEADLINES = "groupNotificationDeadlines"
const val GROUP_NOTIFICATION_FRIENDS = "groupNotificationFriends"

fun Context.themeColor(
    @AttrRes themeAttrId: Int
): Int {
    return obtainStyledAttributes(
        intArrayOf(themeAttrId)
    ).use {
        it.getColor(0, Color.MAGENTA)
    }
}

fun hideKeyboard(activity: Activity) {
    val inputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    // Check if no view has focus
    val currentFocusedView = activity.currentFocus
    currentFocusedView?.let {
        inputMethodManager.hideSoftInputFromWindow(
            currentFocusedView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}

fun NavController.popBackStackAllInstances(destination: Int, inclusive: Boolean): Boolean {
    var popped: Boolean
    while (true) {
        popped = popBackStack(destination, inclusive)
        if (!popped) {
            break
        }
    }
    return popped
}

fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    val bitmap =
        Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

open class BaseTabFragment : Fragment() {
    private var isNavigated = false

    override fun onDestroyView() {
        super.onDestroyView()
        if (!isNavigated)
            requireActivity().onBackPressedDispatcher.addCallback(this) {
                val navController = findNavController()
                if (navController.currentBackStackEntry?.destination?.id != null) {
                    findNavController().popBackStackAllInstances(
                        navController.currentBackStackEntry?.destination?.id!!,
                        true
                    )
                } else
                    navController.popBackStack()
            }
    }
}


