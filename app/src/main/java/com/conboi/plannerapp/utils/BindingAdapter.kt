package com.conboi.plannerapp.utils

import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import coil.load
import com.conboi.plannerapp.R
import com.google.android.material.textfield.TextInputEditText
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("setTextTime")
fun TextInputEditText.setTextTime(time: Int) {
    if (time != GLOBAL_DATE_FOR_CHECK) {
        setText(
            SimpleDateFormat(
                "h:mm a", Locale.getDefault()
            ).format(
                Date(time.toLong() * 1000)
            )
        )
    }
}

@BindingAdapter("setTextNewCompleted")
fun TextView.setTextNewCompleted(completed: Long) {
    text = resources.getString(
        R.string.task_completed,
        DateFormat.getDateTimeInstance().format(completed)
    )
}

@BindingAdapter("setTextNewPriority")
fun AutoCompleteTextView.setTextNewPriority(priority: Int) {
    val items: Array<String> = resources.getStringArray(R.array.priorities)
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
            setTextColor(Color.parseColor("#c62828"))
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


@BindingAdapter("setTvTextTaskChecked")
fun TextView.setTvTextTaskChecked(checked: Boolean) {
    if (checked) {
        paint.isStrikeThruText = true
        isEnabled = false

    } else {
        paint.isStrikeThruText = false
        isEnabled = true

    }
}

@BindingAdapter("setTietTextTaskChecked")
fun TextInputEditText.setTietTextTaskChecked(checked: Boolean) {
    if (checked) {
        paint.isStrikeThruText = true
        isEnabled = false

    } else {
        paint.isStrikeThruText = false
        isEnabled = true

    }
}

@BindingAdapter("setTaskPriority", "setTextTaskChecked", requireAll = true)
fun LinearLayoutCompat.setTaskPriority(priority: Int, checked: Boolean) {
    if (checked) {
        setBackgroundResource(0)
        alpha = 0.5F
    } else {
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
        alpha = 1F
    }
    background?.alpha = 200
}


@BindingAdapter("loadImage")
fun ImageView.loadImage(URL: String?) {
    if (URL != "null") {
        load(URL)
        elevation = 4F
    }else{
        load(R.drawable.ic_baseline_account_circle_24)
    }
}

@BindingAdapter("setFriendRequestStatus")
fun ImageView.setFriendRequestStatus(request_code: Int) {
    when (request_code) {
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

