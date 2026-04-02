package com.conboi.plannerapp.old.ui.bottomsheet

//
// @AndroidEntryPoint
// class BottomNavigationFragment : BottomSheetDialogFragment() {
//    private var _binding: FragmentBottomNavigationBinding? = null
//    val binding get() = _binding!!
//
//    private val viewModel: BottomNavigationViewModel by viewModels()
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding =
//            DataBindingUtil.inflate(inflater, R.layout.fragment_bottom_navigation, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(
//        view: View,
//        savedInstanceState: Bundle?
//    ) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.lifecycleOwner = this
//        binding.user = viewModel.user
//        NavigationUI.setupWithNavController(binding.nvNavigationOptions, findNavController())
//
//        if (viewModel.isEmailVerified.not()) {
//            binding.tvEmail.setTextColor(Color.RED)
//        }
//
//        val navigationMenuHeader = binding.nvNavigationOptions.getChildAt(0) as NavigationMenuView
//        navigationMenuHeader.isVerticalScrollBarEnabled = false
//
//        binding.nvNavigationOptions.setNavigationItemSelectedListener { menuItem ->
//            if (menuItem.isChecked) return@setNavigationItemSelectedListener false
//            return@setNavigationItemSelectedListener when (menuItem.itemId) {
//                R.id.nav_menu_main_fragment -> {
//                    navigateToFragment(R.id.mainFragment)
//                }
//                R.id.nav_menu_friends_fragment -> {
//                    navigateToFragment(R.id.friendsFragment)
//                }
//                R.id.nav_menu_profile_fragment -> {
//                    navigateToFragment(R.id.profileFragment)
//                }
//                else -> true
//            }
//        }
//
//        binding.ivBtnSettings.setOnClickListener {
//            navigateToSettings()
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//    private fun navigateToSettings() {
//        if (findNavController().currentDestination?.id == R.id.settingsFragment) {
//            dismiss()
//            return
//        }
//        val settingsFragment = BottomSettingsFragment()
//        settingsFragment.show(
//            parentFragmentManager,
//            settingsFragment.tag
//        )
//        dismiss()
//    }
//
//    private fun navigateToFragment(fragmentId: Int): Boolean {
//        if (findNavController().currentDestination?.id == fragmentId) {
//            dismiss()
//            return false
//        }
//        findNavController().navigate(fragmentId)
//        dismiss()
//        return true
//    }
// }