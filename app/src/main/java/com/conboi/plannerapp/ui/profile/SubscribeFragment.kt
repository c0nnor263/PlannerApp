package com.conboi.plannerapp.ui.profile

import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.billingclient.api.BillingFlowParams
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.source.remote.repo.FirebaseRepository
import com.conboi.plannerapp.databinding.FragmentSubscribeBinding
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.utils.PremiumType
import com.conboi.plannerapp.utils.isAppInternetConnected
import com.conboi.plannerapp.utils.shared.LoadingDialogFragment
import com.conboi.plannerapp.utils.showErrorToast
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.transition.MaterialSharedAxis
import com.qonversion.android.sdk.Qonversion
import com.qonversion.android.sdk.dto.QEntitlement
import com.qonversion.android.sdk.dto.QEntitlementRenewState
import com.qonversion.android.sdk.dto.QonversionError
import com.qonversion.android.sdk.dto.products.QProduct
import com.qonversion.android.sdk.listeners.QonversionEntitlementsCallback
import com.qonversion.android.sdk.listeners.QonversionProductsCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SubscribeFragment : Fragment() {
    private var _binding: FragmentSubscribeBinding? = null
    val binding get() = _binding!!

    private val viewModel: SubscribeViewModel by viewModels()

    private var bufferSelected = 0
    private var isCancelled = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscribeBinding.inflate(layoutInflater)

        (activity as MainActivity).let { activity ->
            activity.binding.fabMain.setOnClickListener {
                processSubscription()
            }
            activity.binding.fabMain.setOnLongClickListener(null)
            activity.onBackPressedDispatcher.addCallback(this) {
                findNavController().navigateUp()
            }
        }

        binding.mCvStandardOption.setOnClickListener {
            openSubscriptionSettings()
        }
        binding.mCvMonthOption.setOnClickListener {
            viewModel.updateSelectedPremium(1)
        }
        binding.mCvSixMonthOption.setOnClickListener {
            viewModel.updateSelectedPremium(2)
        }
        binding.mCvYearOption.setOnClickListener {
            viewModel.updateSelectedPremium(3)
        }
        binding.tvRestorePurchases.setOnClickListener { restorePurchases() }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        //Enter
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }

        binding.tvBenefits.text =
            Html.fromHtml(
                resources.getString(R.string.benefits),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )


        viewModel.premiumType.observe(viewLifecycleOwner) {
            when (it) {
                PremiumType.STANDARD -> {
                    productsChecked(standard = true)
                    viewModel.updateSelectedPremium(0)
                    bufferSelected = 0
                }
                PremiumType.MONTH -> {
                    productsChecked(month = true)
                    viewModel.updateSelectedPremium(1)
                    bufferSelected = 1
                }
                PremiumType.SIX_MONTH -> {
                    productsChecked(sixMonth = true)
                    viewModel.updateSelectedPremium(2)
                    bufferSelected = 2
                }
                PremiumType.YEAR -> {
                    productsChecked(year = true)
                    viewModel.updateSelectedPremium(3)
                    bufferSelected = 3
                }
                else -> {}
            }
            if (it != PremiumType.STANDARD) showManageSubscriptionUI() else hideManageSubscriptionUI()
        }

        viewModel.selectedPremiumType.observe(viewLifecycleOwner) {
            when (it) {
                0 -> productsChecked(standard = true)
                1 -> productsChecked(month = true)
                2 -> productsChecked(sixMonth = true)
                3 -> productsChecked(year = true)
            }
        }
        setSubscribeProducts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.selectedPremiumType.removeObservers(this)
    }


    private fun showManageSubscriptionUI() {
        binding.tvStandardOptionTitle.text = resources.getString(R.string.manage_subscriptions)
        binding.tvStandardOptionPrice.text = null
        binding.rbStandardOption.visibility = View.INVISIBLE
        binding.mCvStandardOption.setOnClickListener {
            openSubscriptionSettings()
        }
    }

    private fun hideManageSubscriptionUI() {
        binding.tvStandardOptionTitle.text =
            resources.getString(R.string.standard_subscription)
        binding.tvStandardOptionPrice.text = resources.getString(R.string.free)
        binding.rbStandardOption.visibility = View.VISIBLE
        binding.mCvStandardOption.setOnClickListener {
            viewModel.updateSelectedPremium(0)
        }
    }

    private fun productsChecked(
        standard: Boolean = false,
        month: Boolean = false,
        sixMonth: Boolean = false,
        year: Boolean = false
    ) {
        val color = ContextCompat.getColor(
            requireContext(),
            R.color.secondaryLightColorFire
        )

        binding.rbStandardOption.isChecked = standard
        binding.rbMonthOption.isChecked = month
        binding.rbSixMonthOption.isChecked = sixMonth
        binding.rbYearOption.isChecked = year

        binding.sivCheckMark1.visibility = if (bufferSelected == 0) View.VISIBLE else View.GONE
        binding.sivCheckMark2.visibility = if (bufferSelected == 1) View.VISIBLE else View.GONE
        binding.sivCheckMark3.visibility = if (bufferSelected == 2) View.VISIBLE else View.GONE
        binding.sivCheckMark4.visibility = if (bufferSelected == 3) View.VISIBLE else View.GONE

        binding.mCvStandardOption.strokeColor = if (standard) color else Color.TRANSPARENT
        binding.mCvMonthOption.strokeColor = if (month) color else Color.TRANSPARENT
        binding.mCvSixMonthOption.strokeColor = if (sixMonth) color else Color.TRANSPARENT
        binding.mCvYearOption.strokeColor = if (year) color else Color.TRANSPARENT

        val checkMark: ShapeableImageView = when (bufferSelected) {
            0 -> binding.sivCheckMark1
            1 -> binding.sivCheckMark2
            2 -> binding.sivCheckMark3
            3 -> binding.sivCheckMark4
            else -> binding.sivCheckMark1
        }

        if (isCancelled) {
            checkMark.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_help_24
                )
            )
        }
    }

    private fun setSubscribeProducts() {
        val connectivityManager =
            ContextCompat.getSystemService(requireContext(), ConnectivityManager::class.java)
        val isConnected = connectivityManager?.isAppInternetConnected()

        if (isConnected == true) {
            Qonversion.shared.products(object : QonversionProductsCallback {
                override fun onSuccess(products: Map<String, QProduct>) {
                    if (products.isNotEmpty()) {
                        updateContent(products)
                    }
                }

                override fun onError(error: QonversionError) {
                    showErrorToast(requireContext(), Exception(error.additionalMessage))
                }
            })
        } else {
            Toast.makeText(
                requireContext(),
                resources.getString(R.string.check_your_internet),
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigateUp()
        }
    }

    private fun updateContent(products: Map<String, QProduct>) {
        val monthSubscription = products[MainActivity.MONTH_PRODUCT]
        val sixSubscription = products[MainActivity.SIX_MONTH_PRODUCT]
        val yearSubscription = products[MainActivity.YEAR_PRODUCT]

        _binding?.let {
            if (monthSubscription != null) {
                binding.tvMonthOptionTitle.text = resources.getString(R.string.month_subscription)
                binding.tvMonthOptionPrice.text = monthSubscription.prettyPrice
            }

            if (sixSubscription != null) {
                binding.tvSixMonthOptionTitle.text =
                    resources.getString(R.string.six_month_subscription)
                binding.tvSixMonthOptionPrice.text = sixSubscription.prettyPrice
                binding.discount1.paint.isStrikeThruText = true
                binding.discount1.text = resources.getString(R.string.discount_15)
            }

            if (yearSubscription != null) {
                binding.tvYearOptionTitle.text = resources.getString(R.string.year_subscription)
                binding.tvYearOptionPrice.text = yearSubscription.prettyPrice
                binding.discount2.paint.isStrikeThruText = true
                binding.discount2.text = resources.getString(R.string.discount_20)
            }
        }
        Qonversion.shared.syncPurchases()
        Qonversion.shared.checkEntitlements(object : QonversionEntitlementsCallback {
            override fun onSuccess(permissions: Map<String, QEntitlement>) {
                val premiumPermission = permissions[MainActivity.PREMIUM_PERMISSION]

                if (premiumPermission != null && premiumPermission.isActive) {
                    if (premiumPermission.renewState == QEntitlementRenewState.Canceled) {
                        isCancelled = true
                    }
                }
            }

            override fun onError(error: QonversionError) {
                showErrorToast(requireContext(), Exception(error.additionalMessage))
            }
        })
    }

    private fun processSubscription() {
        val selectedPremium = viewModel.selectedPremiumType.value
        if ((selectedPremium != bufferSelected || isCancelled) &&
            selectedPremium != 0
        ) {
            val loadingDialog = LoadingDialogFragment()
            loadingDialog.isCancelable = false
            loadingDialog.show(
                childFragmentManager, LoadingDialogFragment.TAG
            )

            when (selectedPremium) {
                1 -> {
                    if (bufferSelected == 0 || isCancelled) {
                        purchaseSubscription(
                            loadingDialog,
                            MainActivity.MONTH_PRODUCT,
                            PremiumType.MONTH
                        )
                    } else {
                        updatePurchaseSubscription(
                            loadingDialog,
                            MainActivity.MONTH_PRODUCT,
                            PremiumType.MONTH
                        )
                    }
                }
                2 -> {
                    if (bufferSelected == 0 || isCancelled) {
                        purchaseSubscription(
                            loadingDialog,
                            MainActivity.SIX_MONTH_PRODUCT,
                            PremiumType.SIX_MONTH
                        )
                    } else {
                        updatePurchaseSubscription(
                            loadingDialog,
                            MainActivity.SIX_MONTH_PRODUCT,
                            PremiumType.SIX_MONTH
                        )
                    }
                }
                3 -> {
                    if (bufferSelected == 0 || isCancelled) {
                        purchaseSubscription(
                            loadingDialog,
                            MainActivity.YEAR_PRODUCT,
                            PremiumType.YEAR
                        )
                    } else {
                        updatePurchaseSubscription(
                            loadingDialog,
                            MainActivity.YEAR_PRODUCT,
                            PremiumType.YEAR
                        )
                    }
                }
            }
            (activity as MainActivity).checkPermissions()
        } else {
            findNavController().navigateUp()
        }
    }

    private fun purchaseSubscription(
        loadingDialog: LoadingDialogFragment,
        premiumProduct: String,
        newPremiumType: PremiumType
    ) {
        val activity = activity as MainActivity

        Qonversion.shared.purchase(
            activity,
            premiumProduct,
            object : QonversionEntitlementsCallback {
                override fun onSuccess(permissions: Map<String, QEntitlement>) {
                    val premiumPermission =
                        permissions[MainActivity.PREMIUM_PERMISSION]

                    if (premiumPermission != null && premiumPermission.isActive) {
                        viewModel.setNewPremium(
                            newPremiumType,
                            hashMapOf(FirebaseRepository.UserKey.KEY_USER_PREMIUM_TYPE to newPremiumType.name)
                        )
                    }
                    loadingDialog.dismiss()
                    findNavController().navigateUp()
                }

                override fun onError(error: QonversionError) {
                    showErrorToast(
                        requireContext(),
                        Exception(error.additionalMessage)
                    )
                    loadingDialog.dismiss()
                }

            })
    }

    private fun updatePurchaseSubscription(
        loadingDialog: LoadingDialogFragment,
        premiumProduct: String,
        newPremiumType: PremiumType
    ) {
        val activity = activity as MainActivity
        val prorationMode = BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION

        Qonversion.shared.updatePurchase(
            activity,
            premiumProduct,
            when (bufferSelected) {
                1 -> MainActivity.MONTH_PRODUCT
                2 -> MainActivity.SIX_MONTH_PRODUCT
                3 -> MainActivity.YEAR_PRODUCT
                else -> return
            },
            prorationMode,
            object : QonversionEntitlementsCallback {
                override fun onSuccess(permissions: Map<String, QEntitlement>) {
                    val premiumPermission =
                        permissions[MainActivity.PREMIUM_PERMISSION]

                    if (premiumPermission != null && premiumPermission.isActive) {
                        viewModel.setNewPremium(
                            newPremiumType,
                            hashMapOf(FirebaseRepository.UserKey.KEY_USER_PREMIUM_TYPE to newPremiumType.name),
                            true
                        )
                    }
                    loadingDialog.dismiss()
                    findNavController().navigateUp()
                }

                override fun onError(error: QonversionError) {
                    showErrorToast(
                        requireContext(),
                        Exception(error.additionalMessage)
                    )
                    loadingDialog.dismiss()
                }
            })
    }

    private fun restorePurchases() {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.isCancelable = false
        loadingDialog.show(
            childFragmentManager, LoadingDialogFragment.TAG
        )

        Qonversion.shared.restore(object : QonversionEntitlementsCallback {
            override fun onSuccess(permissions: Map<String, QEntitlement>) {
                val premiumPermission = permissions[MainActivity.PREMIUM_PERMISSION]
                if (premiumPermission != null && premiumPermission.isActive) {
                    viewModel.updatePremium(true)
                    (requireActivity() as MainActivity).checkPermissions()
                }
                loadingDialog.dismiss()
                findNavController().navigateUp()
                Toast.makeText(context, R.string.restored_purchases, Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: QonversionError) {
                loadingDialog.dismiss()
                showErrorToast(requireContext(), Exception(error.additionalMessage))
            }
        })
    }


    private fun openSubscriptionSettings() {
        val typeSubscription = when (viewModel.premiumType.value) {
            PremiumType.STANDARD -> null
            PremiumType.MONTH -> MainActivity.MONTH_PRODUCT
            PremiumType.SIX_MONTH -> MainActivity.SIX_MONTH_PRODUCT
            PremiumType.YEAR -> MainActivity.YEAR_PRODUCT
            else -> null
        }

        typeSubscription?.let {
            val uri: Uri =
                Uri.parse("https://play.google.com/store/account/subscriptions?$it&package=${requireContext().packageName}")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}