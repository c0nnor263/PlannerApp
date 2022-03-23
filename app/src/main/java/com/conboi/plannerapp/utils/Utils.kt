package com.conboi.plannerapp.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.use
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.utils.shared.firebase.FirebaseResult
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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

fun Context.themeColor(
    @AttrRes themeAttrId: Int
): Int {
    return obtainStyledAttributes(
        intArrayOf(themeAttrId)
    ).use {
        it.getColor(0, Color.MAGENTA)
    }
}

@ColorRes
fun Context.getColorPrimaryTheme(code: Int): Int {

    fun getColor(uiModeNight: Boolean): Int {
        return when (uiModeNight to code) {
            //Light
            false to MAIN_TAG -> R.color.primaryLightColorWater
            false to FRIENDS_TAG -> R.color.primaryLightColorFire
            false to PROFILE_TAG -> R.color.primaryLightColorTree
            false to (OTHER_COLOR or SETTINGS_TAG) -> R.color.primaryLightColorAir

            //Night
            true to MAIN_TAG -> R.color.secondaryDarkColorWater
            true to FRIENDS_TAG -> R.color.secondaryDarkColorFire
            true to PROFILE_TAG -> R.color.secondaryDarkColorTree
            true to (OTHER_COLOR or SETTINGS_TAG) -> R.color.secondaryDarkColorAir

            else -> if (uiModeNight) R.color.secondaryDarkColorWater else R.color.primaryLightColorWater
        }

    }

    return getColor(
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    )
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

fun ConnectivityManager.isAppInternetConnected(): Boolean {
    val networkCapabilities = activeNetwork ?: return false
    (getNetworkCapabilities(networkCapabilities) ?: return false).let { actvNet ->
        return when {
            actvNet.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actvNet.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actvNet.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
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

fun getUniqueRequestCode(alarmType: AlarmType, id: Int) =
    when (alarmType) {
        AlarmType.REMINDER -> "1111$id".toInt()
        AlarmType.DEADLINE -> "2222$id".toInt()
        else -> 0
    }

fun getTimeFromString(strTime: String?, alarmType: AlarmType) =
    when (alarmType) {
        AlarmType.REMINDER -> strTime?.split(":")?.first() ?: "0"
        AlarmType.DEADLINE -> strTime?.split(":")?.last() ?: "0"
        AlarmType.ALL -> ""
    }

fun getStringTimeWithReset(resetFor: AlarmType, strTime: String?) =
    when (resetFor) {
        AlarmType.REMINDER -> "0:$strTime"
        AlarmType.DEADLINE -> "$strTime:0"
        AlarmType.ALL -> ""
    }

fun showErrorToast(context: Context, exception: Exception?) {
    exception?.let {
        Toast.makeText(
            context,
            context.resources.getString(
                R.string.cant_get_user_friends,
                exception.message.toString()
            ),
            Toast.LENGTH_SHORT
        ).show()
    }
}


fun showAdView(context: Context, adView: AdView, viewToPadding: View, topPadding: Int): AdView {
    adView.visibility = View.VISIBLE
    val scale = context.resources.displayMetrics.density
    viewToPadding.updatePadding(top = (topPadding * scale + 0.5f).toInt())

    adView.loadAd(AdRequest.Builder().build())
    return adView
}

fun hideAdView(adView: AdView, viewToPadding: View, topPadding: Int) {
    adView.visibility = View.GONE
    adView.destroy()
    viewToPadding.updatePadding(top = topPadding)
}

fun showCantCheckDialog(context: Context): AlertDialog =
    MaterialAlertDialogBuilder(context)
        .setMessage(context.resources.getString(R.string.cant_check_task))
        .setNeutralButton(context.resources.getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
        }
        .show()


fun showCantOvercheckDialog(context: Context): AlertDialog =
    MaterialAlertDialogBuilder(context)
        .setMessage(context.resources.getString(R.string.cant_overcheck_task))
        .setNeutralButton(context.resources.getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
        }
        .show()

fun showErrorCheckInternetConnectionDialog(
    context: Context,
    title: String,
    positiveAction: () -> Unit,
    negativeAction: (() -> Unit?)? = null
): AlertDialog =
    MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setMessage(context.resources.getString(R.string.check_your_internet))
        .setPositiveButton(context.resources.getString(R.string.try_word)) { dialog, _ ->
            positiveAction()
            dialog.dismiss()
        }
        .setNegativeButton(context.resources.getString(R.string.cancel)) { dialog, _ ->
            negativeAction?.let { it() }
            dialog.cancel()
        }
        .setCancelable(false)
        .show()


inline fun <T> firebaseCall(action: () -> FirebaseResult<T>): FirebaseResult<T> {
    return try {
        action()
    } catch (e: Exception) {
        FirebaseResult.Error(e)
    }
}
