package com.conboi.plannerapp.utils

import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import coil.load
import coil.transform.CircleCropTransformation
import com.conboi.plannerapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("setPrivateModeState")
fun MaterialButton.setPrivateModeState(privateState: Boolean) {
    text = resources.getString(
        R.string.private_state,
        if (privateState) resources.getString(R.string.on) else resources.getString(R.string.off)
    )
    if (privateState) {
        setBackgroundColor(ContextCompat.getColor(context, R.color.primaryLightColorTree))
    } else {
        setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDarkColorAir))
    }
}

@BindingAdapter("setVibrationModeState")
fun MaterialButton.setVibrationModeState(vibrationState: Boolean) {
    text = resources.getString(
        R.string.vibration_state,
        if (vibrationState) resources.getString(R.string.on) else resources.getString(R.string.off)
    )
    if (vibrationState) {
        setBackgroundColor(ContextCompat.getColor(context, R.color.primaryLightColorTree))
    } else {
        setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDarkColorAir))
    }
}

@BindingAdapter("setRemindersModeState")
fun MaterialButton.setRemindersModeState(reminderState: Boolean) {
    text = resources.getString(
        R.string.reminders_state,
        if (reminderState) resources.getString(R.string.on) else resources.getString(R.string.off)
    )
    if (reminderState) {
        setBackgroundColor(ContextCompat.getColor(context, R.color.primaryLightColorTree))
    } else {
        setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDarkColorAir))
    }
}

@BindingAdapter("setNotificationsModeState")
fun MaterialButton.setNotificationsModeState(notificationsState: Boolean) {
    text = resources.getString(
        R.string.notifications_state,
        if (notificationsState) resources.getString(R.string.on) else resources.getString(R.string.off)
    )
    if (notificationsState) {
        setBackgroundColor(ContextCompat.getColor(context, R.color.primaryLightColorTree))
    } else {
        setBackgroundColor(ContextCompat.getColor(context, R.color.primaryDarkColorAir))
    }
}


@BindingAdapter("setTaskTime")
fun TextInputEditText.setTaskTime(time: Long) {
    if (time != GLOBAL_START_DATE) {
        setText(
            SimpleDateFormat(
                "h:mm a", Locale.getDefault()
            ).format(
                Date(time)
            )
        )
    }
}

@BindingAdapter("setTaskDate")
fun TextInputEditText.setTaskDate(time: Long) {
    if (time != GLOBAL_START_DATE) {
        setText(
            SimpleDateFormat(
                "EEE, d MMM yyyy", Locale.getDefault()
            ).format(
                Date(time)
            )
        )
    }
}

@BindingAdapter("setWholeTaskTime")
fun TextInputEditText.setWholeTaskTime(time: Long) {
    if (time != GLOBAL_START_DATE) {
        setText(
            SimpleDateFormat(
                "EEE, d MMM, h:mm a", Locale.getDefault()
            ).format(
                Date(time)
            )
        )
    }
}




@BindingAdapter("setTextCompleted")
fun TextView.setTextCompleted(completed: Long) {
    text = resources.getString(
        R.string.task_completed,
        DateFormat.getDateTimeInstance().format(completed)
    )
}

@BindingAdapter("setActvPriority")
fun MaterialAutoCompleteTextView.setActvPriority(priority: Int) {
    val items = resources.getStringArray(R.array.priorities)
    when (priority) {
        0 -> {
            setText(items[0])
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primaryDarkColorAir
                )
            )
        }
        1 -> {
            setText(items[1])
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.secondaryColorWater
                )
            )
        }
        2 -> {
            setText(items[2])
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primaryLightColorFire
                )
            )
        }
        3 -> {
            setText(items[3])
            setTextColor(ContextCompat.getColor(context, R.color.secondaryDarkColorFire))
        }
        else -> {
            setText(items[1])
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primaryLightColorWater
                )
            )
        }
    }
    val adapter = ArrayAdapter(
        context,
        R.layout.dropmenu_priority,
        items
    )
    setAdapter(adapter)
}

@BindingAdapter("setTotal")
fun TextView.setTotal(total: Int) {
    if (total > 1) {
        text = if (total < 100) {
            total.toString()
        } else {
            resources.getString(R.string.max_total_check, 99)
        }
        visibility = View.VISIBLE
    } else {
        visibility = View.GONE
    }
}

@BindingAdapter("parentPriority", "parentChecked", "parentTitle", "parentTotal", requireAll = true)
fun ConstraintLayout.parentTask(priority: Int, checked: Boolean, title: String, total: Int) {
    fun setPriorityColor(priority: Int) {
        when (priority) {
            0 -> {
                setBackgroundResource(R.drawable.gradient_priority_leisurely)
            }
            1 -> {
                setBackgroundResource(R.drawable.gradient_priority_default)
            }
            2 -> {
                setBackgroundResource(R.drawable.gradient_priority_advisable)
            }
            3 -> {
                setBackgroundResource(R.drawable.gradient_priority_important)
            }
            else -> setBackgroundResource(R.drawable.gradient_priority_default)
        }
    }
    if (title.isBlank()) {
        setBackgroundResource(R.color.secondaryLightColorWater)
    } else {
        alpha = if (checked) {
            if (total > 1) {
                setPriorityColor(priority)
                0.8F
            } else {
                setBackgroundResource(0)
                0.5F
            }
        } else {
            setPriorityColor(priority)
            1F
        }
    }
    background?.alpha = 200
}


@BindingAdapter("setRepeatMode")
fun MaterialAutoCompleteTextView.setRepeatMode(repeatMode: Int) {
    val items = resources.getStringArray(R.array.repeat_modes)
    when (repeatMode) {
        0 -> {
            setText(items[0])
        }
        1 -> {
            setText(items[1])
        }
        2 -> {
            setText(items[2])
        }
    }
    val adapter = ArrayAdapter(context, R.layout.dropmenu_repeat_modes, items)
    setAdapter(adapter)
}

@BindingAdapter("setLanguage")
fun MaterialAutoCompleteTextView.setLanguage(language: String) {
    val items = resources.getStringArray(R.array.languages)
    when (language) {
        "en" -> {
            setText(items[0])
        }
        "ru" -> {
            setText(items[1])
        }
        "zh" -> {
            setText(items[2])
        }
    }
    val adapter = ArrayAdapter(context, R.layout.dropmenu_language, items)
    setAdapter(adapter)
}

@BindingAdapter("setTextInputEditTextTaskChecked", "setTextInputEditTextTaskTotal")
fun TextInputEditText.setTextInputCheckedTotal(checked: Boolean, total: Int) {
    if (checked) {
        if (total > 1) {
            paint.isStrikeThruText = false
            isEnabled = true
        }else{
            paint.isStrikeThruText = true
            isEnabled = false
        }

    } else {
        paint.isStrikeThruText = false
        isEnabled = true

    }
}


@BindingAdapter("setFriendAdded")
fun TextView.setFriendAdded(addedTime: Long) {
    text = resources.getString(
        R.string.added_friend_time,
        SimpleDateFormat(
            "EEE, d MMM yyyy", Locale.getDefault()
        ).format(
            Date(addedTime)
        )
    )
}

@BindingAdapter("setFriendRequestStatus")
fun ImageView.setFriendRequestStatus(requestCode: Int) {
    when (requestCode) {
        0 -> {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_baseline_pending_24)
        }
        2 -> {
            visibility = View.VISIBLE
            setImageResource(R.drawable.ic_baseline_help_24)
        }
        else -> {
            visibility = View.INVISIBLE
        }
    }
}


@BindingAdapter("loadImage")
fun ImageView.loadImage(url: String) {
    if (url.isNotBlank()) {
        load(url) {
            error(R.drawable.ic_baseline_account_circle_24)
            transformations(CircleCropTransformation())
        }
    } else {
        setImageResource(R.drawable.ic_baseline_account_circle_24)
    }
}


@BindingAdapter("layoutFullscreen")
fun View.bindLayoutFullscreen(previousFullscreen: Boolean, fullscreen: Boolean) {
    if (previousFullscreen != fullscreen && fullscreen) {
        systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}

@BindingAdapter(
    "paddingLeftSystemWindowInsets",
    "paddingTopSystemWindowInsets",
    "paddingRightSystemWindowInsets",
    "paddingBottomSystemWindowInsets",
    requireAll = false
)
fun View.applySystemWindowInsetsPadding(
    previousApplyLeft: Boolean,
    previousApplyTop: Boolean,
    previousApplyRight: Boolean,
    previousApplyBottom: Boolean,
    applyLeft: Boolean,
    applyTop: Boolean,
    applyRight: Boolean,
    applyBottom: Boolean
) {
    if (previousApplyLeft == applyLeft &&
        previousApplyTop == applyTop &&
        previousApplyRight == applyRight &&
        previousApplyBottom == applyBottom
    ) {
        return
    }

    doOnApplyWindowInsets { view, insets, padding, _, _ ->
        val left = if (applyLeft) insets.systemWindowInsetLeft else 0
        val top = if (applyTop) insets.systemWindowInsetTop else 0
        val right = if (applyRight) insets.systemWindowInsetRight else 0
        val bottom = if (applyBottom) insets.systemWindowInsetBottom else 0

        view.setPadding(
            padding.left + left,
            padding.top + top,
            padding.right + right,
            padding.bottom + bottom
        )
    }
}

@BindingAdapter(
    "marginLeftSystemWindowInsets",
    "marginTopSystemWindowInsets",
    "marginRightSystemWindowInsets",
    "marginBottomSystemWindowInsets",
    requireAll = false
)
fun View.applySystemWindowInsetsMargin(
    previousApplyLeft: Boolean,
    previousApplyTop: Boolean,
    previousApplyRight: Boolean,
    previousApplyBottom: Boolean,
    applyLeft: Boolean,
    applyTop: Boolean,
    applyRight: Boolean,
    applyBottom: Boolean
) {
    if (previousApplyLeft == applyLeft &&
        previousApplyTop == applyTop &&
        previousApplyRight == applyRight &&
        previousApplyBottom == applyBottom
    ) {
        return
    }

    doOnApplyWindowInsets { view, insets, _, margin, _ ->
        val left = if (applyLeft) insets.systemWindowInsetLeft else 0
        val top = if (applyTop) insets.systemWindowInsetTop else 0
        val right = if (applyRight) insets.systemWindowInsetRight else 0
        val bottom = if (applyBottom) insets.systemWindowInsetBottom else 0

        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = margin.left + left
            topMargin = margin.top + top
            rightMargin = margin.right + right
            bottomMargin = margin.bottom + bottom
        }
    }
}

fun View.doOnApplyWindowInsets(
    block: (View, WindowInsets, InitialPadding, InitialMargin, Int) -> Unit
) {
    // Create a snapshot of the view's padding & margin states
    val initialPadding = recordInitialPaddingForView(this)
    val initialMargin = recordInitialMarginForView(this)
    val initialHeight = recordInitialHeightForView(this)
    // Set an actual OnApplyWindowInsetsListener which proxies to the given
    // lambda, also passing in the original padding & margin states
    setOnApplyWindowInsetsListener { v, insets ->
        block(v, insets, initialPadding, initialMargin, initialHeight)
        // Always return the insets, so that children can also use them
        insets
    }
    // request some insets
    requestApplyInsetsWhenAttached()
}

class InitialPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

class InitialMargin(val left: Int, val top: Int, val right: Int, val bottom: Int)

private fun recordInitialPaddingForView(view: View) = InitialPadding(
    view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom
)

private fun recordInitialMarginForView(view: View): InitialMargin {
    val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
        ?: throw IllegalArgumentException("Invalid view layout params")
    return InitialMargin(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
}

private fun recordInitialHeightForView(view: View): Int {
    return view.layoutParams.height
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // We're already attached, just request as normal
        requestApplyInsets()
    } else {
        // We're not attached to the hierarchy, add a listener to
        // request when we are
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

