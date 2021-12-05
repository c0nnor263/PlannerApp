package com.conboi.plannerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.databinding.ListFriendBinding
import com.conboi.plannerapp.model.FriendType
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.firebase.firestore.ktx.toObject

class FriendAdapter(
    options: FirestorePagingOptions<FriendType>,
    private val listener: OnFriendListInterface
) :
    FirestorePagingAdapter<FriendType, FriendAdapter.ViewHolder>(options) {
    inner class ViewHolder(private var binding: ListFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                itemView.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val friend = getItem(position)
                        listener.onFriendClick(
                            itemView,
                            binding.friendAvatar,
                            friend!!.toObject<FriendType>()!!
                        )
                    }
                }
                itemView.setOnLongClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val friend = getItem(position)!!.toObject<FriendType>()!!
                        listener.onFriendHold(binding.friendAvatar, friend.user_id, friend.user_name)
                    }
                    true
                }
            }
        }

        fun bind(friend: FriendType) = with(binding) {
            binding.friend = friend
            executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ListFriendBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }


    interface OnFriendListInterface {
        fun onFriendClick(view: View, avatar: ImageView, friend: FriendType)
        fun onFriendHold(view: View, friendId: String, friendName: String)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: FriendType) {
        holder.bind(model)
    }
}


