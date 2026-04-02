package com.conboi.core.domain.source.remote

// TODO: Maybe move to data or network module
sealed class FirebaseResult<T>(val data: T? = null, val error: Exception? = null) {
    class Success<T>(result: T?) : FirebaseResult<T>(data = result)

    class Error<T>(error: Exception?) : FirebaseResult<T>(error = error)
}