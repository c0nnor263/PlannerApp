package com.conboi.plannerapp.utils

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import coil.load
import coil.transform.CircleCropTransformation
import com.conboi.plannerapp.R
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import java.text.DateFormat
import java.util.*


@BindingAdapter("tietSetTaskTime")
fun TextInputEditText.tietSetTaskTime(time: Long) {
    if (time != GLOBAL_START_DATE) {
        val defaultLocale = resources.configuration.locales[0]
        setText(
            DateFormat.getTimeInstance(DateFormat.DEFAULT, defaultLocale)
                .format(
                    Date(time)
                )
        )
    }
}

@BindingAdapter("tietSetTaskDate")
fun TextInputEditText.tietSetTaskDate(time: Long) {
    if (time != GLOBAL_START_DATE) {
        val defaultLocale = resources.configuration.locales[0]
        setText(
            DateFormat.getDateInstance(DateFormat.DEFAULT, defaultLocale)
                .format(
                    Date(time)
                )
        )
    }
}

@BindingAdapter("tietSetFullTaskTime")
fun TextInputEditText.tietSetFullTaskTime(time: Long) {
    if (time != GLOBAL_START_DATE) {
        val defaultLocale = resources.configuration.locales[0]
        setText(
            DateFormat.getDateTimeInstance(
                DateFormat.DEFAULT,
                DateFormat.DEFAULT,
                defaultLocale
            ).format(
                Date(time)
            )
        )
    }
}


@BindingAdapter("mTvSetTextCompleted")
fun MaterialTextView.mTvSetTextCompleted(completed: Long) {
    val defaultLocale = resources.configuration.locales[0]
    text = resources.getString(
        R.string.task_completed,
        DateFormat.getDateTimeInstance(
            DateFormat.DEFAULT,
            DateFormat.DEFAULT,
            defaultLocale
        ).format(completed)
    )
}


@BindingAdapter("mTvSetTextCreated")
fun MaterialTextView.mTvSetTextCreated(created: Long) {
    val defaultLocale = resources.configuration.locales[0]
    text = resources.getString(
        R.string.task_created,
        DateFormat.getDateTimeInstance(
            DateFormat.DEFAULT,
            DateFormat.DEFAULT,
            defaultLocale
        ).format(created)
    )
}

@BindingAdapter("mActvSetPriority")
fun MaterialAutoCompleteTextView.mActvSetPriority(priority: Priority?) {
    val items = resources.getStringArray(R.array.priorities)

    when (priority) {
        Priority.LEISURELY -> {
            setText(items[0])
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primaryDarkColorAir
                )
            )
        }
        Priority.DEFAULT -> {
            setText(items[1])
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.secondaryColorWater
                )
            )
        }
        Priority.ADVISABLE -> {
            setText(items[2])
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.primaryLightColorFire
                )
            )
        }
        Priority.IMPORTANT -> {
            setText(items[3])
            setTextColor(ContextCompat.getColor(context, R.color.secondaryDarkColorFire))
        }
        else -> {
            setText(items[1])
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.secondaryColorWater
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

@BindingAdapter("mTvSetTotal")
fun MaterialTextView.mTvSetTotal(total: Int) {
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

@BindingAdapter(
    "clPriority",
    "clChecked",
    "clTitle",
    "clTotal",
    "clMissed",
    requireAll = true
)
fun ConstraintLayout.clTask(
    priority: Priority?,
    checked: Boolean,
    title: String,
    total: Int,
    missed: Boolean,
) {
    fun setPriorityColor(priority: Priority?) {
        setBackgroundResource(
            when (priority) {
                Priority.LEISURELY -> R.drawable.gradient_priority_leisurely
                Priority.DEFAULT -> R.drawable.gradient_priority_default
                Priority.ADVISABLE -> R.drawable.gradient_priority_advisable
                Priority.IMPORTANT -> R.drawable.gradient_priority_important
                else -> R.drawable.gradient_priority_default
            }
        )
    }

    if (title.isBlank()) {
        alpha = 1.0F
        setBackgroundResource(R.color.secondaryDarkColorWater)
    } else {
        alpha =
            if (checked) {
                if (total > 1) {
                    setPriorityColor(priority)
                    1.0F
                } else {
                    setBackgroundResource(0)
                    0.5F
                }
            } else {
                setPriorityColor(priority)
                1.0F
            }
    }
    background?.alpha = 200
    if (missed) {
        alpha = 1.0F
        setBackgroundResource(R.color.secondaryDarkColorFire)
    }
}


@BindingAdapter("mActvSetRepeatMode")
fun MaterialAutoCompleteTextView.mActvSetRepeatMode(repeatMode: RepeatMode?) {
    val items = resources.getStringArray(R.array.repeat_modes)
    setText(
        when (repeatMode) {
            RepeatMode.Once -> items[0]
            RepeatMode.Daily -> items[1]
            RepeatMode.Weekly -> items[2]
            else -> items[0]
        }
    )

    val adapter = ArrayAdapter(context, R.layout.dropmenu_repeat_modes, items)
    setAdapter(adapter)
}


@BindingAdapter("setTextTaskChecked", "setTextTaskTotal")
fun setCheckedTotal(view: View, checked: Boolean, total: Int) {
    when (view) {
        is TextInputEditText -> {
            view.apply {
                paint.isStrikeThruText =
                    if (checked) {
                        if (total > 1) {
                            isEnabled = true
                            false
                        } else {
                            isEnabled = false
                            true
                        }
                    } else {
                        isEnabled = true
                        false
                    }
            }
        }
        is MaterialTextView -> {
            view.apply {
                paint.isStrikeThruText = if (checked) {
                    if (total > 1) {
                        isEnabled = true
                        false
                    } else {
                        isEnabled = false
                        true
                    }

                } else {
                    isEnabled = true
                    false
                }
            }
        }
    }
}


@BindingAdapter("mTvSetFriendAdded")
fun MaterialTextView.mTvSetFriendAdded(addedTime: Long) {
    val defaultLocale = resources.configuration.locales[0]
    text = resources.getString(
        R.string.added_friend_time,
        DateFormat.getDateInstance(DateFormat.DEFAULT, defaultLocale).format(
            Date(addedTime)
        )
    )
}

@BindingAdapter("ivSetFriendRequestStatus")
fun ImageView.ivSetFriendRequestStatus(requestCode: Int) {

    fun loadImage(imageResource: Int) {
        visibility = View.VISIBLE
        load(imageResource) {
            transformations(CircleCropTransformation())
        }
    }

    when (requestCode) {
        0 -> loadImage(R.drawable.ic_baseline_pending_24)
        1 -> visibility = View.INVISIBLE
        2 -> loadImage(R.drawable.ic_baseline_help_24)
        3 -> loadImage(R.drawable.ic_baseline_cancel_24)
    }
}


@BindingAdapter("ivLoadProfileImage")
fun ImageView.ivLoadProfileImage(url: String?) {
    if (url?.isNotBlank() == true) {
        load(url) {
            error(R.drawable.ic_baseline_account_circle_24)
            transformations(CircleCropTransformation())
        }
    } else {
        setImageResource(R.drawable.ic_baseline_account_circle_24)
    }
}

@Suppress("DEPRECATION")
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
        val left = getInsetsToSide(InsetSide.LEFT, applyLeft, insets)
        val top = getInsetsToSide(InsetSide.TOP, applyTop, insets)
        val right = getInsetsToSide(InsetSide.RIGHT, applyRight, insets)
        val bottom = getInsetsToSide(InsetSide.BOTTOM, applyBottom, insets)

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
        val left = getInsetsToSide(InsetSide.LEFT, applyLeft, insets)
        val top = getInsetsToSide(InsetSide.TOP, applyTop, insets)
        val right = getInsetsToSide(InsetSide.RIGHT, applyRight, insets)
        val bottom = getInsetsToSide(InsetSide.BOTTOM, applyBottom, insets)

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

@Suppress("DEPRECATION")
private fun getInsetsToSide(side: InsetSide, applySide: Boolean, insets: WindowInsets): Int {
    return if (applySide) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (side) {
                InsetSide.LEFT -> insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).left
                InsetSide.TOP -> insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).top
                InsetSide.RIGHT -> insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).right
                InsetSide.BOTTOM -> insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).bottom
            }
        } else {
            when (side) {
                InsetSide.LEFT -> insets.systemWindowInsetLeft
                InsetSide.TOP -> insets.systemWindowInsetTop
                InsetSide.RIGHT -> insets.systemWindowInsetRight
                InsetSide.BOTTOM -> insets.systemWindowInsetBottom
            }
        }
    } else 0
}

class InitialPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

class InitialMargin(val left: Int, val top: Int, val right: Int, val bottom: Int)