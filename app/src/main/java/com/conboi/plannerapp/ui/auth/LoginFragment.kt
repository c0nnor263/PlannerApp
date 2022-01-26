package com.conboi.plannerapp.ui.auth

import android.app.Activity
import android.content.Context
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
import com.conboi.plannerapp.ui.IntroActivity
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.APP_FILE
import com.conboi.plannerapp.utils.FIRST_LAUNCH
import com.conboi.plannerapp.utils.popBackStackAllInstances
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.qonversion.android.sdk.QUserProperties
import com.qonversion.android.sdk.Qonversion
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {
    companion object {
        const val SIGN_IN_RESULT_CODE = 2638
    }

    private var mInterstitialAd: InterstitialAd? = null

    var authPrompted = false
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
        if (!authPrompted) {
            launchSignInFLow()
        }
    }

    private fun launchSignInFLow() {
        authPrompted = true
        InterstitialAd.load(
            requireContext(),
            resources.getString(R.string.login_ad_unit_id),
            AdRequest.Builder().build(),
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

            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                navigateToMain()
            }

            override fun onAdShowedFullScreenContent() {
                navigateToMain()
                mInterstitialAd = null
            }

        }
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
                .setIsSmartLockEnabled(false)
                .setLogo(R.drawable.ic_logo)
                .setTheme(R.style.PlannerApp_Login_Theme)
                .build(), SIGN_IN_RESULT_CODE
        )

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in user.
                if (mInterstitialAd != null) {
                    mInterstitialAd?.show(requireActivity())
                }
                navigateToMain()
            } else {
                // Sign in failed. If response is null the user canceled the sign-in flow using
                // the back button. Otherwise check response.getError().getErrorCode() and handle
                // the error.
                if (response == null) {
                    requireActivity().finish()
                }

                if (response?.error?.errorCode == ErrorCodes.NO_NETWORK) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(
                            resources.getString(
                                R.string.unsuccessful_sign_in,
                                response.error?.errorCode
                            )
                        )
                        .setMessage(
                            resources.getString(R.string.check_your_internet)
                        )
                        .setPositiveButton(resources.getString(R.string.try_word)) { dialog, _ ->
                            launchSignInFLow()
                            dialog.dismiss()
                        }
                        .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                            dialog.cancel()
                        }
                        .setCancelable(false)
                        .show()
                }

                if (response?.error?.errorCode == ErrorCodes.UNKNOWN_ERROR) {
                    Toast.makeText(
                        context,
                        resources.getString(
                            R.string.unsuccessful_sign_in,
                            response.error?.errorCode
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                    launchSignInFLow()
                }

            }
        }
    }

    fun navigateToMain() {
        val sharedPref =
            requireActivity().getSharedPreferences(APP_FILE, Context.MODE_PRIVATE) ?: return
        val firstLaunch = sharedPref.getBoolean(FIRST_LAUNCH, true)

        Toast.makeText(
            context,
            resources.getString(
                R.string.successfully_sign_in,
                FirebaseAuth.getInstance().currentUser?.displayName
            ),
            Toast.LENGTH_SHORT
        ).show()
        val id = FirebaseAuth.getInstance().currentUser!!.uid
        Qonversion.setProperty(QUserProperties.CustomUserId, id)
        Qonversion.identify(id)

        findNavController().popBackStackAllInstances(
            findNavController().currentBackStackEntry?.destination?.id!!,
            true
        )
        findNavController().navigate(R.id.mainFragment)
        if (firstLaunch) {
            val intent = Intent(requireContext(), IntroActivity::class.java)
            startActivity(intent)
        }
    }

}