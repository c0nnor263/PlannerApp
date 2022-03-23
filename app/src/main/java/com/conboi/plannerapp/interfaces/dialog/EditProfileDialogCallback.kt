package com.conboi.plannerapp.interfaces.dialog

import com.google.firebase.auth.FirebaseUser

interface EditProfileDialogCallback {
    fun resetUser(user: FirebaseUser)
    fun verifyBeforeUpdateEmail(newEmail: String)
    fun updatePassword(newPassword: String)
    fun resetPassword()
}