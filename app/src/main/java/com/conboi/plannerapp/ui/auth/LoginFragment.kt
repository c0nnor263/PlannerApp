package com.conboi.plannerapp.ui.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentLoginBinding
import com.conboi.plannerapp.ui.IntroActivity
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.utils.showErrorCheckInternetConnectionDialog
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var mInterstitialAd: InterstitialAd? = null

    private val viewModel: LoginViewModel by viewModels()

    private val signInResultLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Successfully signed in user.
                navigateToMain()
            } else {
                errorActions(result.idpResponse)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLoginBinding.inflate(layoutInflater)
        launchSignInFLow()
        return binding.root
    }

    private fun launchSignInFLow() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            requireContext(),
            resources.getString(R.string.login_ad_unit_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    super.onAdLoaded(interstitialAd)
                    mInterstitialAd = interstitialAd
                }
            }
        )

        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                navigateToMain()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                navigateToMain()
            }

            override fun onAdShowedFullScreenContent() {
                navigateToMain()
                mInterstitialAd = null
            }
        }


        val authProviders = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(authProviders)
            .setIsSmartLockEnabled(false)
            .setLogo(R.drawable.ic_logo)
            .setTheme(R.style.PlannerApp_Login_Theme)
            .build()
        signInResultLauncher.launch(intent)
    }

    @SuppressLint("SwitchIntDef")
    private fun errorActions(response: IdpResponse?) {
        val errorCodeString = resources.getString(
            R.string.unsuccessful_sign_in,
            response?.error?.errorCode
        )
        when (response?.error?.errorCode) {
            ErrorCodes.NO_NETWORK -> {
                showErrorCheckInternetConnectionDialog(
                    requireContext(),
                    errorCodeString,
                    { launchSignInFLow() }
                )
            }

            ErrorCodes.UNKNOWN_ERROR -> {
                Toast.makeText(
                    context,
                    errorCodeString,
                    Toast.LENGTH_SHORT
                ).show()
                launchSignInFLow()
            }
        }
        if (response == null) {
            requireActivity().finish()
        }
    }

    fun navigateToMain() {
        val user = Firebase.auth.currentUser
        lifecycleScope.launch {
            val isFirstLaunch = viewModel.isFirstLaunch()
            if (isFirstLaunch) {
                val intent = Intent(requireContext(), IntroActivity::class.java)
                startActivity(intent)
            }
            viewModel.checkForNewUser(user!!, requireContext())

            if (mInterstitialAd != null) {
                mInterstitialAd?.show(requireActivity())
            }

            Toast.makeText(
                context,
                resources.getString(
                    R.string.successfully_sign_in,
                    user.displayName
                ),
                Toast.LENGTH_SHORT
            ).show()

            findNavController().popBackStack()
            findNavController().navigate(R.id.mainFragment)
            (activity as MainActivity).loginInitApp()
        }
    }
}