package com.conboi.features.main

//
// class InputTaskAmountDialogFragment(
//    val callback: InputTaskAmountCallback
// ) : DialogFragment() {
//    private var _binding: FragmentInputTaskAmountDialogBinding? = null
//    val binding get() = _binding!!
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        _binding = FragmentInputTaskAmountDialogBinding.inflate(layoutInflater)
//
//        val thisDialog =
//            MaterialAlertDialogBuilder(
//                requireContext()
//            )
//                .setView(binding.root)
//                .setTitle(resources.getString(R.string.enter_amount_title))
//                .setPositiveButton(R.string.add_task) { _, _ ->
//                    val inputAmountString = binding.inputAmount.text.toString()
//
//                    callback.beginInsert(inputAmountString)
//                    dismiss()
//                }
//                .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
//                .create()
//
//        return thisDialog
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View = binding.root
//
//    override fun onViewCreated(
//        view: View,
//        savedInstanceState: Bundle?
//    ) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.inputAmountLayout.hint =
//            resources.getString(
//                R.string.enter_amount_hint,
//                MAX_ADD_TASK
//            )
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//    companion object {
//        const val TAG = "InputTaskAmountDialogFragment"
//    }
// }