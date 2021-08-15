package com.conboi.plannerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.R
import com.conboi.plannerapp.data.FriendType
import com.conboi.plannerapp.databinding.ListFriendBinding
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

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
                layoutListFriendCardView.setOnClickListener {
                    listener.onFriendClick(bindingAdapterPosition)
                }
            }
        }

        fun bind(model: FriendType) = with(binding) {
            if (model.idFriend == "Add") {
                layoutListFriendCardView.setCardBackgroundColor(0)
                listFriendAdd.visibility = View.VISIBLE
                listFriendName.visibility = View.INVISIBLE
            } else {

                layoutListFriendCardView.setCardBackgroundColor(R.drawable.water_fragment_background)
                listFriendAdd.visibility = View.INVISIBLE
                listFriendName.visibility = View.VISIBLE
                listFriendName.text = model.idFriend
            }
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

