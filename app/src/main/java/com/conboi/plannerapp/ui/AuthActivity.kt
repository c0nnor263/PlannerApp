package com.conboi.plannerapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth


class AuthActivity : AppCompatActivity() {

    private val AUTH_REQUEST_CODE = 2638
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.conboi.plannerapp.R.layout.activity_auth)

        firebaseAuth = FirebaseAuth.getInstance()

        listener = FirebaseAuth.AuthStateListener { p0 ->
            if (p0.currentUser != null) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(
                            arrayListOf(
                                AuthUI.IdpConfig.GoogleBuilder().build(),
                                AuthUI.IdpConfig.EmailBuilder().build()
                            )
                        )
                        .setTosAndPrivacyPolicyUrls(
                            "https://superapp.example.com/terms-of-service.html",
                            "https://superapp.example.com/privacy-policy.html"
                        )
                        .setIsSmartLockEnabled(false,true)
                        .setTheme(com.conboi.plannerapp.R.style.AirTheme_PlannerApp)
                        .build(), AUTH_REQUEST_CODE
                )

            }
        }
    }


    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    override fun onStop() {
        if(listener!=null) {
            firebaseAuth.removeAuthStateListener(listener)
        }
        super.onStop()
    }
}