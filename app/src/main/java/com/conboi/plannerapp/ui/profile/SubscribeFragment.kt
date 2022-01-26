package com.conboi.plannerapp.ui.profile

import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.billingclient.api.BillingFlowParams
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.PreferencesManager
import com.conboi.plannerapp.data.PremiumType
import com.conboi.plannerapp.databinding.FragmentSubscribeBinding
import com.conboi.plannerapp.ui.MainActivity
import com.conboi.plannerapp.ui.main.LoadingAlertFragment
import com.conboi.plannerapp.ui.main.MainFragment
import com.conboi.plannerapp.ui.main.SharedViewModel
import com.conboi.plannerapp.utils.APP_FILE
import com.conboi.plannerapp.utils.IMPORT_CONFIRM
import com.conboi.plannerapp.utils.RESUBSCRIBE_ALERT
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import com.qonversion.android.sdk.Qonversion
import com.qonversion.android.sdk.QonversionError
import com.qonversion.android.sdk.QonversionPermissionsCallback
import com.qonversion.android.sdk.QonversionProductsCallback
import com.qonversion.android.sdk.dto.QPermission
import com.qonversion.android.sdk.dto.products.QProduct
import com.qonversion.android.sdk.dto.products.QProductRenewState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class SubscribeFragment @Inject constructor() : Fragment() {
    @Inject
    lateinit var preferencesManager: PreferencesManager

    private var _binding: FragmentSubscribeBinding? = null
    val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var bufferSelected = 0
    private var isCancelled = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_subscribe, container, false)

        binding.standardOptionParent.setOnClickListener {
            sharedViewModel.selectedPremiumType.value = 0
        }
        binding.monthOptionParent.setOnClickListener {
            sharedViewModel.selectedPremiumType.value = 1
        }
        binding.sixMonthOptionParent.setOnClickListener {
            sharedViewModel.selectedPremiumType.value = 2
        }
        binding.yearOptionParent.setOnClickListener {
            sharedViewModel.selectedPremiumType.value = 3
        }
        binding.restorePurchases.setOnClickListener { restorePurchases() }

        (requireActivity() as MainActivity).checkPermissions()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigateUp()
        }
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        //Out enter
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        reenterTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }

        //Enter
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }

        (activity as MainActivity).binding.bottomFloatingButton.apply {
            setOnClickListener { purchaseSubscription() }
            setOnLongClickListener(null)
        }

        binding.benefits.text =
            Html.fromHtml(resources.getString(R.string.benefits), HtmlCompat.FROM_HTML_MODE_LEGACY)

        setSubscribeOptions()

    }

    private fun setSubscribeOptions() {
        Qonversion.products(object : QonversionProductsCallback {
            override fun onSuccess(products: Map<String, QProduct>) {
                updateContent(products)
                (requireActivity() as MainActivity).checkPermissions()
            }

            override fun onError(error: QonversionError) {
                Toast.makeText(requireContext(), error.additionalMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateContent(products: Map<String, QProduct>) {

        val monthSubscription = products[MainActivity.MONTH_PRODUCT]
        val sixSubscription = products[MainActivity.SIX_MONTH_PRODUCT]
        val yearSubscription = products[MainActivity.YEAR_PRODUCT]

        if (monthSubscription != null) {
            binding.monthOptionTitle.text = resources.getString(R.string.month_subscription)
            binding.monthOptionPrice.text = monthSubscription.prettyPrice
        }

        if (sixSubscription != null) {
            binding.sixMonthOptionTitle.text = resources.getString(R.string.six_month_subscription)
            binding.sixMonthOptionPrice.text = sixSubscription.prettyPrice
            binding.discount1.paint.isStrikeThruText = true
            binding.discount1.text = resources.getString(R.string.discount_15)
        }

        if (yearSubscription != null) {
            binding.yearOptionTitle.text = resources.getString(R.string.year_subscription)
            binding.yearOptionPrice.text = yearSubscription.prettyPrice
            binding.discount2.paint.isStrikeThruText = true
            binding.discount2.text = resources.getString(R.string.discount_20)

        }
        Qonversion.syncPurchases()
        Qonversion.checkPermissions(object : QonversionPermissionsCallback {
            override fun onSuccess(permissions: Map<String, QPermission>) {
                val premiumPermission = permissions[MainActivity.PREMIUM_PERMISSION]

                if (premiumPermission != null && premiumPermission.isActive()) {
                    if (premiumPermission.renewState == QProductRenewState.Canceled) {
                        isCancelled = true
                    }
                }
            }

            override fun onError(error: QonversionError) {
                Toast.makeText(requireContext(), error.description, Toast.LENGTH_SHORT).show()
            }
        })


        sharedViewModel.premiumType.observe(this.viewLifecycleOwner) {
            val color = ContextCompat.getColor(
                requireContext(),
                R.color.secondaryLightColorFire
            )
            when (it) {
                PremiumType.STANDARD -> {
                    sharedViewModel.selectedPremiumType.value = 0
                    binding.standardOptionTitle.text =
                        resources.getString(R.string.standard_subscription)
                    binding.standardOptionPrice.text = resources.getString(R.string.free)
                    binding.standardOptionRadio.visibility = View.VISIBLE
                    binding.standardOptionParent.setOnClickListener {
                        sharedViewModel.selectedPremiumType.value = 0
                    }


                    if (bufferSelected == 0) {
                        bufferSelected = 0
                        binding.standardOptionParent.strokeColor = color
                        binding.standardOptionParent.strokeWidth = 8

                        binding.monthOptionParent.strokeColor = Color.TRANSPARENT
                        binding.sixMonthOptionParent.strokeColor = Color.TRANSPARENT
                        binding.yearOptionParent.strokeColor = Color.TRANSPARENT


                        binding.checkMark1.visibility = View.VISIBLE
                        binding.checkMark2.visibility = View.GONE
                        binding.checkMark3.visibility = View.GONE
                        binding.checkMark4.visibility = View.GONE

                        binding.monthOptionParent.strokeColor = Color.TRANSPARENT
                        binding.sixMonthOptionParent.strokeColor = Color.TRANSPARENT
                        binding.yearOptionParent.strokeColor = Color.TRANSPARENT
                    }
                }
                PremiumType.MONTH -> {
                    sharedViewModel.selectedPremiumType.value = 1
                    binding.standardOptionTitle.text =
                        resources.getString(R.string.manage_subscriptions)
                    binding.standardOptionPrice.text = null
                    binding.standardOptionRadio.visibility = View.INVISIBLE
                    binding.standardOptionParent.setOnClickListener {
                        openSubscriptionScreen()
                    }

                    if (bufferSelected == 0) {
                        if (isCancelled) {
                            binding.checkMark2.setImageDrawable(
                                ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_baseline_help_24
                                )
                            )
                        }
                        bufferSelected = 1
                        binding.monthOptionParent.strokeColor = color
                        binding.monthOptionParent.strokeWidth = 8

                        binding.checkMark1.visibility = View.GONE
                        binding.checkMark2.visibility = View.VISIBLE
                        binding.checkMark3.visibility = View.GONE
                        binding.checkMark4.visibility = View.GONE

                        binding.standardOptionParent.strokeColor = Color.TRANSPARENT
                        binding.sixMonthOptionParent.strokeColor = Color.TRANSPARENT
                        binding.yearOptionParent.strokeColor = Color.TRANSPARENT
                    }
                }
                PremiumType.SIX_MONTH -> {
                    sharedViewModel.selectedPremiumType.value = 2
                    binding.standardOptionTitle.text =
                        resources.getString(R.string.manage_subscriptions)
                    binding.standardOptionPrice.text = null
                    binding.standardOptionRadio.visibility = View.INVISIBLE
                    binding.standardOptionParent.setOnClickListener {
                        openSubscriptionScreen()
                    }

                    if (bufferSelected == 0) {
                        if (isCancelled) {
                            binding.checkMark3.setImageDrawable(
                                ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_baseline_help_24
                                )
                            )
                        }
                        bufferSelected = 2
                        binding.sixMonthOptionParent.strokeColor = color
                        binding.sixMonthOptionParent.strokeWidth = 8

                        binding.checkMark1.visibility = View.GONE
                        binding.checkMark2.visibility = View.GONE
                        binding.checkMark3.visibility = View.VISIBLE
                        binding.checkMark4.visibility = View.GONE

                        binding.standardOptionParent.strokeColor = Color.TRANSPARENT
                        binding.monthOptionParent.strokeColor = Color.TRANSPARENT
                        binding.yearOptionParent.strokeColor = Color.TRANSPARENT
                    }
                }
                PremiumType.YEAR -> {
                    sharedViewModel.selectedPremiumType.value = 3
                    binding.standardOptionTitle.text =
                        resources.getString(R.string.manage_subscriptions)
                    binding.standardOptionPrice.text = null
                    binding.standardOptionRadio.visibility = View.INVISIBLE
                    binding.standardOptionParent.setOnClickListener {
                        openSubscriptionScreen()
                    }

                    if (bufferSelected == 0) {
                        if (isCancelled) {
                            binding.checkMark4.setImageDrawable(
                                ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_baseline_help_24
                                )
                            )
                        }

                        bufferSelected = 3
                        binding.yearOptionParent.strokeColor = color
                        binding.yearOptionParent.strokeWidth = 8

                        binding.checkMark1.visibility = View.GONE
                        binding.checkMark2.visibility = View.GONE
                        binding.checkMark3.visibility = View.GONE
                        binding.checkMark4.visibility = View.VISIBLE

                        binding.standardOptionParent.strokeColor = Color.TRANSPARENT
                        binding.monthOptionParent.strokeColor = Color.TRANSPARENT
                        binding.sixMonthOptionParent.strokeColor = Color.TRANSPARENT
                    }
                }
                null -> {
                    sharedViewModel.selectedPremiumType.value = 0
                }
            }
        }

        sharedViewModel.selectedPremiumType.observe(this.viewLifecycleOwner) {
            when (it) {
                0 -> {
                    binding.standardOptionRadio.isChecked = true
                    binding.monthOptionRadio.isChecked = false
                    binding.sixMonthOptionRadio.isChecked = false
                    binding.yearOptionRadio.isChecked = false
                }
                1 -> {
                    binding.standardOptionRadio.isChecked = false
                    binding.monthOptionRadio.isChecked = true
                    binding.sixMonthOptionRadio.isChecked = false
                    binding.yearOptionRadio.isChecked = false
                }
                2 -> {
                    binding.standardOptionRadio.isChecked = false
                    binding.monthOptionRadio.isChecked = false
                    binding.sixMonthOptionRadio.isChecked = true
                    binding.yearOptionRadio.isChecked = false
                }
                3 -> {
                    binding.standardOptionRadio.isChecked = false
                    binding.monthOptionRadio.isChecked = false
                    binding.sixMonthOptionRadio.isChecked = false
                    binding.yearOptionRadio.isChecked = true
                }
            }
        }
    }

    private fun openSubscriptionScreen() {
        val typeSubscription = when (sharedViewModel.premiumType.value) {
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

    private fun purchaseSubscription() {
        val prorationMode = BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION
        if ((sharedViewModel.selectedPremiumType.value != bufferSelected || isCancelled) &&
            sharedViewModel.selectedPremiumType.value != 0
        ) {
            val activity = activity as MainActivity
            val loadingDialog = LoadingAlertFragment()
            loadingDialog.show(
                childFragmentManager, LoadingAlertFragment.TAG
            )

            when (sharedViewModel.selectedPremiumType.value) {
                1 -> {
                    if (bufferSelected == 0 || isCancelled) {
                        Qonversion.purchase(activity,
                            MainActivity.MONTH_PRODUCT, object : QonversionPermissionsCallback {
                                override fun onSuccess(permissions: Map<String, QPermission>) {
                                    val premiumPermission =
                                        permissions[MainActivity.PREMIUM_PERMISSION]
                                    if (premiumPermission != null && premiumPermission.isActive()) {
                                        sharedViewModel.updatePremiumType(PremiumType.MONTH)
                                        activity.getSharedPreferences(
                                            APP_FILE,
                                            Context.MODE_PRIVATE
                                        )
                                            .edit()
                                            .putBoolean(
                                                IMPORT_CONFIRM, false
                                            )
                                            .putBoolean(
                                                RESUBSCRIBE_ALERT, false
                                            )
                                            .apply()
                                        activity.db.document("Users/${activity.auth.currentUser?.uid}")
                                            .update(
                                                hashMapOf<String, Any>(MainFragment.KEY_USER_PREMIUM_TYPE to PremiumType.MONTH.name)
                                            )

                                    }
                                    loadingDialog.dismiss()
                                    findNavController().navigateUp()
                                }

                                override fun onError(error: QonversionError) {
                                    Toast.makeText(
                                        context,
                                        error.description,
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                    loadingDialog.dismiss()
                                }

                            })
                    } else {
                        Qonversion.updatePurchase(
                            activity,
                            MainActivity.MONTH_PRODUCT,
                            when (bufferSelected) {
                                1 -> {
                                    MainActivity.MONTH_PRODUCT
                                }
                                2 -> {
                                    MainActivity.SIX_MONTH_PRODUCT
                                }
                                3 -> {
                                    MainActivity.YEAR_PRODUCT
                                }
                                else -> {
                                    return
                                }
                            },
                            prorationMode,
                            object : QonversionPermissionsCallback {
                                override fun onSuccess(permissions: Map<String, QPermission>) {
                                    val premiumPermission =
                                        permissions[MainActivity.PREMIUM_PERMISSION]
                                    if (premiumPermission != null && premiumPermission.isActive()) {
                                        sharedViewModel.updatePremiumType(PremiumType.MONTH)
                                        activity.getSharedPreferences(
                                            APP_FILE,
                                            Context.MODE_PRIVATE
                                        )
                                            .edit()
                                            .putBoolean(
                                                IMPORT_CONFIRM, false
                                            )
                                            .putBoolean(
                                                RESUBSCRIBE_ALERT, false
                                            )
                                            .apply()
                                        activity.db.document("Users/${activity.auth.currentUser?.uid}")
                                            .update(
                                                hashMapOf<String, Any>(MainFragment.KEY_USER_PREMIUM_TYPE to PremiumType.MONTH.name)
                                            )

                                    }
                                    loadingDialog.dismiss()
                                    findNavController().navigateUp()
                                }

                                override fun onError(error: QonversionError) {
                                    Toast.makeText(
                                        context,
                                        error.description,
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                    loadingDialog.dismiss()
                                }

                            })
                    }
                }
                2 -> {
                    if (bufferSelected == 0 || isCancelled) {
                        Qonversion.purchase(activity,
                            MainActivity.SIX_MONTH_PRODUCT, object : QonversionPermissionsCallback {
                                override fun onSuccess(permissions: Map<String, QPermission>) {
                                    val premiumPermission =
                                        permissions[MainActivity.PREMIUM_PERMISSION]
                                    if (premiumPermission != null && premiumPermission.isActive()) {
                                        sharedViewModel.updatePremiumType(PremiumType.SIX_MONTH)
                                        activity.getSharedPreferences(
                                            APP_FILE,
                                            Context.MODE_PRIVATE
                                        )
                                            .edit()
                                            .putBoolean(
                                                IMPORT_CONFIRM, false
                                            )
                                            .putBoolean(
                                                RESUBSCRIBE_ALERT, false
                                            )
                                            .apply()
                                        activity.db.document("Users/${activity.auth.currentUser?.uid}")
                                            .update(
                                                hashMapOf<String, Any>(MainFragment.KEY_USER_PREMIUM_TYPE to PremiumType.SIX_MONTH.name)
                                            )

                                    }
                                    loadingDialog.dismiss()
                                    findNavController().navigateUp()
                                }

                                override fun onError(error: QonversionError) {
                                    Toast.makeText(context, error.description, Toast.LENGTH_LONG)
                                        .show()
                                    loadingDialog.dismiss()
                                }
                            })
                    } else {
                        Qonversion.updatePurchase(
                            activity,
                            MainActivity.SIX_MONTH_PRODUCT,
                            when (bufferSelected) {
                                1 -> {
                                    MainActivity.MONTH_PRODUCT
                                }
                                2 -> {
                                    MainActivity.SIX_MONTH_PRODUCT
                                }
                                3 -> {
                                    MainActivity.YEAR_PRODUCT
                                }
                                else -> {
                                    return
                                }
                            },
                            prorationMode,
                            object : QonversionPermissionsCallback {
                                override fun onSuccess(permissions: Map<String, QPermission>) {
                                    val premiumPermission =
                                        permissions[MainActivity.PREMIUM_PERMISSION]
                                    if (premiumPermission != null && premiumPermission.isActive()) {
                                        sharedViewModel.updatePremiumType(PremiumType.SIX_MONTH)
                                        activity.getSharedPreferences(
                                            APP_FILE,
                                            Context.MODE_PRIVATE
                                        )
                                            .edit()
                                            .putBoolean(
                                                IMPORT_CONFIRM, false
                                            )
                                            .putBoolean(
                                                RESUBSCRIBE_ALERT, false
                                            )
                                            .apply()
                                        activity.db.document("Users/${activity.auth.currentUser?.uid}")
                                            .update(
                                                hashMapOf<String, Any>(MainFragment.KEY_USER_PREMIUM_TYPE to PremiumType.SIX_MONTH.name)
                                            )

                                    }
                                    loadingDialog.dismiss()
                                    findNavController().navigateUp()
                                }

                                override fun onError(error: QonversionError) {
                                    Toast.makeText(
                                        context,
                                        error.description,
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                    loadingDialog.dismiss()
                                }

                            })
                    }
                }
                3 -> {
                    if (bufferSelected == 0 || isCancelled) {
                        Qonversion.purchase(activity,
                            MainActivity.YEAR_PRODUCT, object : QonversionPermissionsCallback {
                                override fun onSuccess(permissions: Map<String, QPermission>) {
                                    val premiumPermission =
                                        permissions[MainActivity.PREMIUM_PERMISSION]
                                    if (premiumPermission != null && premiumPermission.isActive()) {
                                        sharedViewModel.updatePremiumType(PremiumType.YEAR)
                                        activity.getSharedPreferences(
                                            APP_FILE,
                                            Context.MODE_PRIVATE
                                        )
                                            .edit()
                                            .putBoolean(
                                                IMPORT_CONFIRM, false
                                            )
                                            .putBoolean(
                                                RESUBSCRIBE_ALERT, false
                                            )
                                            .apply()
                                        activity.db.document("Users/${activity.auth.currentUser?.uid}")
                                            .update(
                                                hashMapOf<String, Any>(MainFragment.KEY_USER_PREMIUM_TYPE to PremiumType.YEAR.name)
                                            )

                                    }
                                    loadingDialog.dismiss()
                                    findNavController().navigateUp()
                                }

                                override fun onError(error: QonversionError) {
                                    Toast.makeText(context, error.description, Toast.LENGTH_LONG)
                                        .show()
                                    loadingDialog.dismiss()
                                }
                            })
                    } else {
                        Qonversion.updatePurchase(
                            activity,
                            MainActivity.YEAR_PRODUCT,
                            when (bufferSelected) {
                                1 -> {
                                    MainActivity.MONTH_PRODUCT
                                }
                                2 -> {
                                    MainActivity.SIX_MONTH_PRODUCT
                                }
                                3 -> {
                                    MainActivity.YEAR_PRODUCT
                                }
                                else -> {
                                    return
                                }
                            },
                            prorationMode,
                            object : QonversionPermissionsCallback {
                                override fun onSuccess(permissions: Map<String, QPermission>) {
                                    val premiumPermission =
                                        permissions[MainActivity.PREMIUM_PERMISSION]
                                    if (premiumPermission != null && premiumPermission.isActive()) {
                                        sharedViewModel.updatePremiumType(PremiumType.YEAR)
                                        activity.getSharedPreferences(
                                            APP_FILE,
                                            Context.MODE_PRIVATE
                                        )
                                            .edit()
                                            .putBoolean(
                                                IMPORT_CONFIRM, false
                                            )
                                            .putBoolean(
                                                RESUBSCRIBE_ALERT, false
                                            )
                                            .apply()
                                        activity.db.document("Users/${activity.auth.currentUser?.uid}")
                                            .update(
                                                hashMapOf<String, Any>(MainFragment.KEY_USER_PREMIUM_TYPE to PremiumType.YEAR.name)
                                            )
                                    }
                                    loadingDialog.dismiss()
                                    findNavController().navigateUp()
                                }

                                override fun onError(error: QonversionError) {
                                    Toast.makeText(
                                        context,
                                        error.description,
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                    loadingDialog.dismiss()
                                }

                            })
                    }
                }
            }
            activity.checkPermissions()
        } else {
            findNavController().navigateUp()
        }

    }

    private fun restorePurchases() {
        val loadingView: View = LayoutInflater.from(context)
            .inflate(
                R.layout.alertdialog_downloading_tasks,
                view as ViewGroup?,
                false
            )
        val loadingTasksAlertDialog = MaterialAlertDialogBuilder(requireContext())
            .setCancelable(false)
            .setTitle(resources.getString(R.string.processing))
            .setView(loadingView)

        val loadingDialog = loadingTasksAlertDialog.create()
        loadingDialog.show()

        Qonversion.restore(object : QonversionPermissionsCallback {
            override fun onSuccess(permissions: Map<String, QPermission>) {
                val premiumPermission = permissions[MainActivity.PREMIUM_PERMISSION]
                if (premiumPermission != null && premiumPermission.isActive()) {
                    (requireActivity() as MainActivity).checkPermissions()
                }
                loadingDialog.dismiss()
                findNavController().navigateUp()
                Toast.makeText(context, R.string.restored_purchases, Toast.LENGTH_SHORT).show()

            }

            override fun onError(error: QonversionError) {
                loadingDialog.dismiss()
                Toast.makeText(context, error.description, Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        sharedViewModel.selectedPremiumType.removeObservers(this)
    }

}