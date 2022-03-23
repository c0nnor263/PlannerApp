package com.conboi.plannerapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.data.model.FriendType
import com.conboi.plannerapp.databinding.ListFriendBinding
import com.conboi.plannerapp.interfaces.FriendListInterface
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.ktx.toObject

class FriendAdapter(
    options: FirestorePagingOptions<FriendType>,
    private val listener: FriendListInterface,
) : FirestorePagingAdapter<FriendType, FriendAdapter.ViewHolder>(options) {

    inner class ViewHolder(private var binding: ListFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val friend = getItem(position)
                    listener.onClick(
                        friend!!.toObject<FriendType>()!!
                    )
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val friend = getItem(position)!!.toObject<FriendType>()!!
                    listener.onHold(
                        binding.ivAvatar,
                        friend.user_id
                    )
                }
                true
            }
        }

        fun bind(friend: FriendType) {
            binding.friend = friend
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ListFriendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: FriendType) =
        holder.bind(model)
}