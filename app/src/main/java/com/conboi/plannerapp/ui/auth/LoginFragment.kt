package com.conboi.plannerapp.ui.auth

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment(R.layout.fragment_login) {
    companion object {
        const val LOGIN_SUCCESSFUL: String = "LOGIN_SUCCESSFUL"
    }

    private lateinit var auth: FirebaseAuth

    private lateinit var savedStateHandle: SavedStateHandle

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
            findNavController().navigate(R.id.bottom_menu_water_fragment)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentLoginBinding.bind(view)
        savedStateHandle = findNavController().currentBackStackEntry!!.savedStateHandle
        savedStateHandle.set(LOGIN_SUCCESSFUL, false)
        auth = Firebase.auth


        binding.apply {
            btnLogin.setOnClickListener {
                when {
                    TextUtils.isEmpty(edLoginEmail.text.toString().trim { it <= ' ' }) -> {
                        Toast.makeText(
                            context,
                            "Please enter email",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    TextUtils.isEmpty(edLoginPassword.text.toString().trim { it <= ' ' }) -> {
                        Toast.makeText(
                            context,
                            "Please enter password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        val email: String = edLoginEmail.text.toString().trim { it <= ' ' }
                        val password: String =
                            edLoginPassword.text.toString().trim { it <= ' ' }
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "You are logged in successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    savedStateHandle.set(LOGIN_SUCCESSFUL, true)
                                    val directions =
                                        LoginFragmentDirections.actionLoginFragmentToBottomMenuWaterFragment(
                                            userId = FirebaseAuth.getInstance().currentUser!!.uid,
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
            tvRegister.setOnClickListener {
                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToRegisterFragment())
            }
        }

    }

}