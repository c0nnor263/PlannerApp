package com.conboi.core.data.old

// TODO: Move to Login feature module
// class FirebaseUserLiveData : LiveData<FirebaseUser?>() {
//    enum class AuthenticationState {
//        AUTHENTICATED,
//        UNAUTHENTICATED,
//    }
//
//    private val firebaseAuth = FirebaseAuth.getInstance()
//
//    private val authStateListener =
//        FirebaseAuth.AuthStateListener { firebaseAuth ->
//            value = firebaseAuth.currentUser
//        }
//
//    override fun onActive() {
//        firebaseAuth.addAuthStateListener(authStateListener)
//    }
//
//    override fun onInactive() {
//        firebaseAuth.removeAuthStateListener(authStateListener)
//    }
// }