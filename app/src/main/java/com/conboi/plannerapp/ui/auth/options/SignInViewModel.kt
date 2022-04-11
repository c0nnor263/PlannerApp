package com.conboi.plannerapp.ui.auth.options

import androidx.lifecycle.*
import com.conboi.plannerapp.data.source.remote.repo.FirebaseRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    val firebaseRepository: FirebaseRepository
) : ViewModel() {
    private val _bufferEmail = MutableLiveData("")
    val bufferEmail: LiveData<String> = _bufferEmail

    private val _bufferPassword = MutableLiveData("")
    val bufferPassword: LiveData<String> = _bufferPassword

    fun updateBufferEmail(value: String) {
        _bufferEmail.value = value
    }

    fun updateBufferPassword(value: String) {
        _bufferPassword.value = value
    }


    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        callback: (FirebaseUser?, Exception?) -> Unit
    ) = firebaseRepository.signInWithEmailAndPassword(email, password, callback)

    fun signInWithGoogleCredential(
        firebaseCredential: AuthCredential,
        callback: (FirebaseUser?, Exception?) -> Unit
    ) {
        firebaseRepository.signInWithGoogleCredential(firebaseCredential, callback)
    }

    fun sendResetPasswordEmail(email: String, callback: (Any?, Exception?) -> Unit) =
        viewModelScope.launch {
            val result = firebaseRepository.sendResetPasswordEmail(email)
            if (result.error == null) {
                callback(null, null)
            } else {
                callback(null, result.error)
            }
        }


    fun saveState() {
        savedStateHandle.apply {
            set(SIGN_IN_EMAIL_VALUE, bufferEmail.value)
            set(SIGN_IN_PASSWORD_VALUE, bufferPassword.value)
        }
    }

    fun retrieveState() {
        savedStateHandle.apply {
            get<String>(SIGN_IN_EMAIL_VALUE)?.let { updateBufferEmail(it) }
            get<String>(SIGN_IN_PASSWORD_VALUE)?.let { updateBufferPassword(it) }
        }
    }

    companion object {
        const val SIGN_IN_EMAIL_VALUE = "SIGN_IN_EMAIL_VALUE"
        const val SIGN_IN_PASSWORD_VALUE = "SIGN_IN_PASSWORD_VALUE"
    }

}