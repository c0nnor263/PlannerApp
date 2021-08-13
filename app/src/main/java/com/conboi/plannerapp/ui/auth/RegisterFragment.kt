package com.conboi.plannerapp.ui.auth

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class RegisterFragment : Fragment(R.layout.fragment_register) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentRegisterBinding.bind(view)

        binding.apply {
            btnRegister.setOnClickListener {
                when {
                    TextUtils.isEmpty(edRegisterEmail.text.toString().trim { it <= ' ' }) -> {
                        Toast.makeText(
                            context,
                            "Please enter email",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    TextUtils.isEmpty(edRegisterPassword.text.toString().trim { it <= ' ' }) -> {
                        Toast.makeText(
                            context,
                            "Please enter password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        val email: String = edRegisterEmail.text.toString().trim { it <= ' ' }
                        val password: String =
                            edRegisterPassword.text.toString().trim { it <= ' ' }
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {

                                    val firebaseUser: FirebaseUser = task.result!!.user!!

                                    Toast.makeText(
                                        context,
                                        "You are registered successfuly",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val directions =
                                        RegisterFragmentDirections.actionRegisterFragmentToBottomMenuWaterFragment(
                                            userId = firebaseUser.uid,
                                            emailId = email
                                        )
                                    findNavController().navigate(directions)
                                } else {
                                    Toast.makeText(
                                        context,
                                        task.exception!!.message.toString(),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                }
            }
            tvLogin.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }
}