package com.conboi.plannerapp.utils.myclass

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager

class NoPreAnGridLayoutManager(
    context: Context?,
    spanCount: Int,
    orientation: Int,
    reverseLayout: Boolean
) : GridLayoutManager(context, spanCount, orientation, reverseLayout) {
    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }
}