package com.conboi.core.domain

import com.conboi.core.domain.source.remote.FirebaseResult

inline fun <T> firebaseCall(action: () -> FirebaseResult<T>): FirebaseResult<T> {
    return try {
        action()
    } catch (e: Exception) {
        FirebaseResult.Error(e)
    }
}