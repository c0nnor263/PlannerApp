package com.conboi.features.friend

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.conboi.plannerapp.databinding.FragmentInviteFriendDialogBinding
import com.conboi.plannerapp.interfaces.dialog.InviteFriendDialogCallback
import com.conboi.plannerapp.utils.InviteFriendError
import com.conboi.plannerapp.utils.showErrorToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class InviteFriendDialogFragment(val callback: InviteFriendDialogCallback) : DialogFragment() {
    private var _binding: FragmentInviteFriendDialogBinding? = null
    val binding get() = _binding!!

    private val viewModel: FriendsViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentInviteFriendDialogBinding.inflate(layoutInflater)

        val thisDialog =
            MaterialAlertDialogBuilder(requireContext())
                .setView(binding.root)
                .setTitle(resources.getString(R.string.send_friend))
                .setPositiveButton(resources.getString(R.string.add), null)
                .setNegativeButton(resources.getString(R.string.cancel), null)
                .create()

        thisDialog.setOnShowListener { dialog ->
            val positiveButton =
                (dialog as AlertDialog).getButton(
                    AlertDialog.BUTTON_POSITIVE,
                )
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setOnClickListener {
                val enteredId = binding.inputId.text.toString().trim()

                if (enteredId.isNotBlank()) {
                    viewModel.inviteFriend(
                        userPrivateState = viewModel.privateState.value ?: false,
                        id = enteredId,
                    ) { _, error ->
                        if (error == null) {
                            callback.successAddedFriend()
                            dismiss()
                        } else {
                            binding.inputIdLayout.error =
                                when (error.message) {
                                    InviteFriendError.ADD_YOURSELF.name -> {
                                        resources.getString(R.string.you_add_friend_yourself)
                                    }

                                    InviteFriendError.FRIEND_ALREADY.name -> {
                                        resources.getString(R.string.error_adding_friend_is_list)
                                    }

                                    InviteFriendError.NOT_EXIST.name -> {
                                        resources.getString(R.string.friend_is_not_exist)
                                    }

                                    else -> {
                                        showErrorToast(requireContext(), error)
                                        resources.getString(R.string.error_loading_friends)
                                    }
                                }
                            binding.inputId.text = null
                        }
                    }
                } else {
                    binding.inputIdLayout.error =
                        resources.getString(R.string.enter_id_alert)
                }
            }
            negativeButton.setOnClickListener {
                dialog.cancel()
            }
        }
        return thisDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = binding.root

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.inputId.addTextChangedListener {
            if (it?.toString()?.isNotBlank() == true) {
                binding.inputIdLayout.error = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "InviteFriendDialogFragment"
    }
}
