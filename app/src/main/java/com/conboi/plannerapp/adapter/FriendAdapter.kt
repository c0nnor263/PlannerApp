package com.conboi.plannerapp.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.FriendType
import com.conboi.plannerapp.databinding.ListFriendBinding
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class FriendAdapter(options: FirestoreRecyclerOptions<FriendType>,
private val listener:OnFriendListInterface) :
    FirestoreRecyclerAdapter<FriendType, FriendAdapter.ViewHolder>(options) {
fun deleteFriend(position: Int){
    snapshots.getSnapshot(position).reference.delete()
}

    interface OnFriendListInterface{
        fun onFriendClick(position: Int)
    }

    inner class ViewHolder(private var binding: ListFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                layoutListFriendConstraintLayout.setOnClickListener {
                    listener.onFriendClick(bindingAdapterPosition)
                }
            }
        }

        fun bind(model: FriendType) = with(binding) {
            if (model.user_id == "Add") {
               layoutListFriendConstraintLayout.setBackgroundColor(0)
                listFriendAdd.visibility = View.VISIBLE
                listFriendName.visibility = View.INVISIBLE
                requestStatus.visibility = View.INVISIBLE
            } else {
                layoutListFriendConstraintLayout.setBackgroundColor(R.drawable.water_fragment_background)
                listFriendAdd.visibility = View.INVISIBLE
                listFriendName.visibility = View.VISIBLE
                listFriendName.text = model.user_name
                if(model.user_request_code == 0){
                    requestStatus.visibility = View.VISIBLE
                    requestStatus.setImageResource(android.R.drawable.ic_menu_recent_history)
                }else if(model.user_request_code == 2){
                    requestStatus.visibility = View.VISIBLE
                    requestStatus.setImageResource(android.R.drawable.ic_menu_help)
                }
                else{
                    requestStatus.visibility = View.INVISIBLE
                }


                if(model.user_photo_url != null){
                   ImageLoadTask(model.user_photo_url, friendAvatar)
                        .execute()
                }
            }
        }
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


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendAdapter.ViewHolder {
        return ViewHolder(
            ListFriendBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: FriendType) {
        holder.bind(model)
    }


}

