package com.conboi.plannerapp.utils.shared

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.firestore.paging.FirestorePagingAdapter

/**
 * Class [GridLayoutManager] was overridden for fix an error with [FirestorePagingAdapter] that comes
 * from standard [GridLayoutManager] with enabled [supportsPredictiveItemAnimations] feature.
 */
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