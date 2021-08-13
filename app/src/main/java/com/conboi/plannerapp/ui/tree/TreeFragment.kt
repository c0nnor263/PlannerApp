package com.conboi.plannerapp.ui.tree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.conboi.plannerapp.R
import com.conboi.plannerapp.databinding.FragmentTreeBinding
import com.conboi.plannerapp.utils.hideKeyboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TreeFragment : Fragment() {
    private var _binding: FragmentTreeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val viewModel: TreeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        binding.apply {
            userId.text = auth.currentUser!!.email
            userEmail.text = auth.currentUser!!.uid
            btnLogout.setOnClickListener {
                auth.signOut()
                findNavController().navigate(R.id.loginFragment)
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        _binding = null
    }
}