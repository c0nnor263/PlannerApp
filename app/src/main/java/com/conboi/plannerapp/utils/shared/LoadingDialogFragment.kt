package com.conboi.plannerapp.utils.shared

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.conboi.plannerapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoadingDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setCancelable(false)
            .setTitle(resources.getString(R.string.processing))
            .setView(R.layout.fragment_loading_dialog)
            .create()

    companion object {
        const val TAG = "LoadingDialogFragment"
    }
}