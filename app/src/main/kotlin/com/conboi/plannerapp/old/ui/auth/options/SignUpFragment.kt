package com.conboi.plannerapp.old.ui.auth.options

//
// @AndroidEntryPoint
// class SignUpFragment : Fragment() {
//    private var _binding: FragmentSignUpBinding? = null
//    val binding get() = _binding!!
//
//    private val viewModel: SignUpViewModel by activityViewModels()
//    private val signViewModel: LoginViewModel by activityViewModels()
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentSignUpBinding.inflate(layoutInflater)
//        binding.lifecycleOwner = this
//        binding.viewModel = viewModel
//
//        binding.mBtnSignUp.setOnClickListener {
//            createUserWithEmailAndPassword()
//        }
//
//        binding.tietName.addTextChangedListener {
//            if (it.toString().isNotBlank()) {
//                viewModel.updateBufferName(it.toString())
//            }
//        }
//        binding.tietEmail.addTextChangedListener {
//            if (it.toString().isNotBlank()) {
//                viewModel.updateBufferEmail(it.toString())
//            }
//        }
//        binding.tietPassword.addTextChangedListener {
//            if (it.toString().isNotBlank()) {
//                viewModel.updateBufferPassword(it.toString())
//            }
//        }
//        binding.tietRepeatPassword.addTextChangedListener {
//            if (it.toString().isNotBlank()) {
//                viewModel.updateBufferRepeatPassword(it.toString())
//            }
//        }
//
//        return binding.root
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        viewModel.saveState()
//    }
//
//    override fun onViewStateRestored(savedInstanceState: Bundle?) {
//        super.onViewStateRestored(savedInstanceState)
//        viewModel.retrieveState()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//    private fun createUserWithEmailAndPassword() {
//        val loadingDialog = LoadingDialogFragment()
//        loadingDialog.show(parentFragmentManager, LoadingDialogFragment.TAG)
//
//        var passCheck = true
//
//        val displayName = binding.tietName.text.toString().trim()
//        val email = binding.tietEmail.text.toString().trim()
//        val password = binding.tietPassword.text.toString().trim()
//        val repeatPassword = binding.tietRepeatPassword.text.toString().trim()
//
//        binding.tilName.error = null
//        binding.tilEmail.error = null
//        binding.tilPassword.error = null
//        binding.tilRepeatPassword.error = null
//
//        if (displayName.isBlank()) {
//            binding.tilName.error = resources.getString(R.string.enter_name)
//            passCheck = false
//        }
//
//        if (!isEmailValid(email)) {
//            binding.tilEmail.error = resources.getString(R.string.enter_email_alert)
//            passCheck = false
//        }
//
//        if (password.isBlank()) {
//            binding.tilPassword.error = resources.getString(R.string.enter_password_alert)
//            passCheck = false
//        } else if (password.length < 8) {
//            binding.tilPassword.error = resources.getString(R.string.enter_password_length_alert)
//            passCheck = false
//        } else {
//            if (repeatPassword.isBlank() || repeatPassword != password) {
//                binding.tilRepeatPassword.error = resources.getString(R.string.enter_password_same)
//                passCheck = false
//            }
//        }
//
//        if (passCheck) {
//            viewModel.createUserWithEmailAndPassword(displayName, email, password) { user, createUserError ->
//                if (createUserError == null) {
//                    Toast.makeText(
//                        requireContext(),
//                        resources.getString(R.string.you_sent_email_confirm),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    viewModel.sendConfirmationEmail { _, error ->
//                        if (error == null) {
//                            signViewModel.sendSuccessSignInEvent(user)
//                        } else {
//                            showErrorToast(requireContext(), error)
//                        }
//                    }
//                    loadingDialog.dismiss()
//                } else {
//                    signViewModel.sendErrorSignInEvent(createUserError)
//                    loadingDialog.dismiss()
//                }
//            }
//        } else {
//            loadingDialog.dismiss()
//        }
//    }
// }