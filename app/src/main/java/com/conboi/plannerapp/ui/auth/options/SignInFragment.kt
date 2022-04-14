package com.conboi.plannerapp.ui.auth.options

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentSignInBinding
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.auth.LoginViewModel
import com.conboi.plannerapp.utils.isEmailValid
import com.conboi.plannerapp.utils.shared.LoadingDialogFragment
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null
    val binding get() = _binding!!

    private val viewModel: SignInViewModel by activityViewModels ()
    private val signViewModel: LoginViewModel by activityViewModels()

    private val signInRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                processSignIn(it.data)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.mBtnSignIn.setOnClickListener {
            signInWithEmailAndPassword()
        }
        binding.tvForgotPassword.setOnClickListener {
            sendResetPasswordEmail()
        }
        binding.btnGoogleSignIn.apply {
            setOnClickListener {
                beginGoogleSignIn()
            }
        }

        binding.tietEmail.addTextChangedListener {
            if (it.toString().isNotBlank()) {
                viewModel.updateBufferEmail(it.toString())
            }
        }
        binding.tietPassword.addTextChangedListener {
            if (it.toString().isNotBlank()) {
                viewModel.updateBufferPassword(it.toString())
            }
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveState()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewModel.retrieveState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun signInWithEmailAndPassword() {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.show(parentFragmentManager, LoadingDialogFragment.TAG)

        var passCheck = true
        val email = binding.tietEmail.text.toString().trim()
        val password = binding.tietPassword.text.toString().trim()

        binding.tilEmail.error = null
        binding.tilPassword.error = null


        if (!isEmailValid(email)) {
            binding.tilEmail.error = resources.getString(R.string.enter_email_alert)
            passCheck = false
        }

        if (password.isBlank()) {
            binding.tilPassword.error = resources.getString(R.string.enter_password_alert)
            passCheck = false
        } else if (password.length < 8) {
            binding.tilPassword.error = resources.getString(R.string.enter_password_length_alert)
            passCheck = false
        }


        if (passCheck) {
            viewModel.signInWithEmailAndPassword(email, password) { user, error ->
                if (error == null) {
                    signViewModel.sendSuccessSignInEvent(user)
                    loadingDialog.dismiss()
                } else {
                    signViewModel.sendErrorSignInEvent(error)
                    loadingDialog.dismiss()
                }
            }
        } else {
            loadingDialog.dismiss()
        }
    }

    private fun beginGoogleSignIn() {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.show(parentFragmentManager, LoadingDialogFragment.TAG)

        val activity = requireActivity() as MainActivity
        activity.beginGoogleSignIn { result, error ->
            if (error == null) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(result!!.pendingIntent.intentSender)
                            .build()
                    signInRequestLauncher.launch(intentSenderRequest)
                    loadingDialog.dismiss()
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("TAG", "Couldn't start One Tap UI: ${e.localizedMessage}")
                    loadingDialog.dismiss()
                }
            }
        }
    }

    private fun processSignIn(data: Intent?) {
        try {
            val credential =
                (activity as MainActivity).oneTapClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken
            when {
                idToken != null -> {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    viewModel.signInWithGoogleCredential(firebaseCredential) { user, error ->
                        if (error == null) {
                            signViewModel.sendSuccessSignInEvent(user)
                        } else {
                            signViewModel.sendErrorSignInEvent(error)
                        }
                    }
                }
                else -> {
                    Log.d("TAG", "No ID token or password!")
                }
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                CommonStatusCodes.CANCELED -> {
                    Log.d("TAG", "One-tap dialog was closed.")
                    // Don't re-prompt the user.
                }
                CommonStatusCodes.NETWORK_ERROR -> {
                    Log.d("TAG", "One-tap encountered a network error.")
                    // Try again or just ignore.
                }
                else -> {
                    Log.d(
                        "TAG", "Couldn't get credential from result." +
                                " (${e.localizedMessage})"
                    )
                }
            }
        }
    }

    private fun sendResetPasswordEmail() {
        val email = binding.tietEmail.text.toString()
        if (isEmailValid(email)) {
            binding.tilEmail.error = null

            viewModel.sendResetPasswordEmail(email) { _, error ->
                if (error == null) {
                    Toast.makeText(
                        requireContext(),
                        resources.getString(R.string.password_reset_sent),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        resources.getString(R.string.password_reset_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            binding.tilEmail.error = resources.getString(R.string.enter_email_alert)
        }
    }
}