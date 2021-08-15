package com.conboi.plannerapp.ui.tree

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.conboi.plannerapp.databinding.FragmentTreeBinding
import com.conboi.plannerapp.ui.AuthActivity
import com.conboi.plannerapp.utils.hideKeyboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Bitmap

import android.graphics.BitmapFactory

import android.os.AsyncTask
import android.widget.ImageView
import com.conboi.plannerapp.ui.water.IMPORT_CONFIRM
import com.conboi.plannerapp.ui.water.WaterSharedViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.InputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL


@ExperimentalCoroutinesApi
@AndroidEntryPoint
class TreeFragment : Fragment() {
    private var _binding: FragmentTreeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val viewModel: TreeViewModel by viewModels()
    val waterViewModel:WaterSharedViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTreeBinding.inflate(inflater, container, false)
        return binding.root
    }
    class ImageLoadTask(private val url: String, imageView: ImageView) :
        AsyncTask<Void?, Void?, Bitmap?>() {
        private val imageView: ImageView = imageView

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            imageView.setImageBitmap(result)
        }

        override fun doInBackground(vararg p0: Void?): Bitmap? {
            try {
                val urlConnection = URL(url)
                val connection: HttpURLConnection = urlConnection
                    .openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                return BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        auth = Firebase.auth
        binding.apply {
            if(auth.currentUser!!.photoUrl != null){
                ImageLoadTask(auth.currentUser!!.photoUrl.toString(), profilePhoto).execute()
            }
            userName.text = auth.currentUser!!.displayName
            userId.text = auth.currentUser!!.uid
            userEmail.text = auth.currentUser!!.email
            btnLogout.setOnClickListener {
                auth.signOut()
                val intent = Intent(context, AuthActivity::class.java)

                waterViewModel.logoutDeleteTasks()
                with(sharedPref.edit()) {
                    putBoolean(IMPORT_CONFIRM,false)
                    apply()
                }
                startActivity(intent)
                requireActivity().finish()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard(requireActivity())
        _binding = null
    }
}