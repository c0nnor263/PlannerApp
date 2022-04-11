package com.conboi.plannerapp.ui.auth.options

import androidx.lifecycle.*
import com.conboi.plannerapp.data.source.remote.repo.FirebaseRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle, val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _bufferName = MutableLiveData("")
    val bufferName: LiveData<String> = _bufferName

    private val _bufferEmail = MutableLiveData("")
    val bufferEmail: LiveData<String> = _bufferEmail

    private val _bufferPassword = MutableLiveData("")
    val bufferPassword: LiveData<String> = _bufferPassword

    private val _bufferRepeatPassword = MutableLiveData("")
    val bufferRepeatPassword: LiveData<String> = _bufferRepeatPassword


    fun updateBufferName(value: String) {
        _bufferName.value = value
    }

    fun updateBufferEmail(value: String) {
        _bufferEmail.value = value
    }

    fun updateBufferPassword(value: String) {
        _bufferPassword.value = value
    }

    fun updateBufferRepeatPassword(value: String) {
        _bufferRepeatPassword.value = value
    }

    fun createUserWithEmailAndPassword(
        displayName:String,
        email: String,
        password: String,
        callback: (FirebaseUser?, Exception?) -> Unit
    ) {
        firebaseRepository.createUserWithEmailAndPassword(displayName,email, password, callback)
    }

    fun sendConfirmationEmail(callback: (Any?, Exception?) -> Unit) =
        viewModelScope.launch {
            val result = firebaseRepository.sendConfirmationEmail()
            if (result.error == null) {
                callback(null, null)
            } else {
                callback(null, result.error)
            }
        }

    fun saveState() {
        savedStateHandle.apply {
            set(SIGN_UP_NAME_VALUE, bufferName.value)
            set(SIGN_UP_EMAIL_VALUE, bufferEmail.value)
            set(SIGN_UP_PASSWORD_VALUE, bufferPassword.value)
            set(SIGN_UP_REPEAT_PASSWORD_VALUE, bufferRepeatPassword.value)
        }
    }

    fun retrieveState() {
        savedStateHandle.apply {
            get<String>(SIGN_UP_NAME_VALUE)?.let { updateBufferName(it) }
            get<String>(SIGN_UP_EMAIL_VALUE)?.let { updateBufferEmail(it) }
            get<String>(SIGN_UP_PASSWORD_VALUE)?.let { updateBufferPassword(it) }
            get<String>(SIGN_UP_REPEAT_PASSWORD_VALUE)?.let { updateBufferRepeatPassword(it) }
        }
    }

    companion object {
        const val SIGN_UP_NAME_VALUE = "SIGN_UP_NAME_VALUE"
        const val SIGN_UP_EMAIL_VALUE = "SIGN_UP_EMAIL_VALUE"
        const val SIGN_UP_PASSWORD_VALUE = "SIGN_UP_PASSWORD_VALUE"
        const val SIGN_UP_REPEAT_PASSWORD_VALUE = "SIGN_UP_REPEAT_PASSWORD_VALUE"
    }
}