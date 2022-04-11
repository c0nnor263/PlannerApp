package com.conboi.plannerapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.adapter.LoginViewPagerAdapter
import com.conboi.plannerapp.databinding.FragmentLoginBinding
import com.conboi.plannerapp.ui.IntroActivity
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.utils.showErrorToast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs


@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    val binding get() = _binding!!

    private var mInterstitialAd: InterstitialAd? = null

    private val viewModel: LoginViewModel by activityViewModels()

    private lateinit var mAdapter: LoginViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, enabled = false) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        initAd()
        initViewPager()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginViewModel.LoginEvent.Success -> {
                            navigateToMain(state.user)
                            viewModel.sendSuccessSignInEvent(null, null)
                        }
                        is LoginViewModel.LoginEvent.Error -> {
                            showErrorToast(requireContext(), state.exception)
                            viewModel.sendErrorSignInEvent(null, null)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun initViewPager() {
        mAdapter = LoginViewPagerAdapter(childFragmentManager, lifecycle)
        binding.viewPager.adapter = mAdapter
        binding.viewPager.setPageTransformer { page, position ->
            page.apply {
                val pageWidth = width
                val pageHeight = height
                when {
                    position <= 1 -> { // [-1,1]
                        // Modify the default slide transition to shrink the page as well
                        val scaleFactor = 0.5f.coerceAtLeast(1 - abs(position))

                        val verticalMargin = pageHeight * (1 - scaleFactor) / 2
                        val horizontalMargin = pageWidth * (1 - scaleFactor) / 2

                        translationY = if (position < 0) {
                            horizontalMargin - verticalMargin / 2
                        } else {
                            horizontalMargin + verticalMargin / 2
                        }
                    }
                }
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text =
                        resources.getString(com.firebase.ui.firestore.R.string.common_signin_button_text)
                }
                1 -> {
                    tab.text = resources.getString(R.string.sign_up)
                }
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initAd() {
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
            override fun onAdShowedFullScreenContent() {
                mInterstitialAd = null
            }
        }
    }


    private fun navigateToMain(user: FirebaseUser?) {
        lifecycleScope.launch {
            viewModel.checkForNewUser(user, requireContext())

            if (mInterstitialAd != null) {
                mInterstitialAd?.show(requireActivity())
            }

            Toast.makeText(
                context,
                resources.getString(
                    R.string.successfully_sign_in,
                    user?.displayName
                ),
                Toast.LENGTH_SHORT
            ).show()


            val isFirstLaunch = viewModel.isFirstLaunch()
            if (isFirstLaunch) {
                val intent = Intent(requireContext(), IntroActivity::class.java)
                startActivity(intent)
            }

            findNavController().popBackStack()
            findNavController().navigate(R.id.mainFragment)
            (activity as MainActivity).loginInitApp()
        }
    }
}