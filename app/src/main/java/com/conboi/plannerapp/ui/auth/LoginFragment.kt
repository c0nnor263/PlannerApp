package com.conboi.plannerapp.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentLoginBinding
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.popBackStackAllInstances
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {
    companion object {
        const val SIGN_IN_RESULT_CODE = 2638
    }

    val sharedViewModel: SharedViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLoginBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        launchSignInFLow()
    }

    private fun launchSignInFLow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.EmailBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(
                    providers
                )
                .setIsSmartLockEnabled(false, true)
                .setLogo(R.drawable.ic_logo)
                .setTheme(R.style.PlannerApp_Login_Theme)
                .setTosAndPrivacyPolicyUrls(
                    "https://firebase.google.com/terms",
                    "https://policies.google.com/u/0/privacy"
                )
                .build(), SIGN_IN_RESULT_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in user.
                Toast.makeText(
                    context,
                    resources.getString(
                        R.string.successfully_sign_in,
                        FirebaseAuth.getInstance().currentUser?.displayName
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStackAllInstances(
                    findNavController().currentBackStackEntry?.destination?.id!!,
                    true
                )
                findNavController().navigate(R.id.mainFragment)

            } else {
                // Sign in failed. If response is null the user canceled the sign-in flow using
                // the back button. Otherwise check response.getError().getErrorCode() and handle
                // the error.
                Toast.makeText(
                    context,
                    resources.getString(R.string.unsuccessful_sign_in, response?.error?.errorCode),
                    Toast.LENGTH_SHORT
                ).show()
                launchSignInFLow()
            }
        }
    }

}