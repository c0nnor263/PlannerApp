package com.conboi.plannerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conboi.plannerapp.data.model.FriendType
import com.conboi.plannerapp.databinding.ListFriendBinding

class FriendAdapter(
    private val listener: OnFriendListInterface
) :
    ListAdapter<FriendType, FriendAdapter.ViewHolder>(FriendDiffCallBack()) {

    inner class ViewHolder(private var binding: ListFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {
                itemView.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val friend = getItem(position)
                        listener.onFriendClick(itemView, friend)
                    }
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
        fun onFriendClick(view: View, friend: FriendType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class FriendDiffCallBack : DiffUtil.ItemCallback<FriendType>() {
    override fun areContentsTheSame(oldItem: FriendType, newItem: FriendType): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(oldItem: FriendType, newItem: FriendType): Boolean {
        return oldItem.user_id == newItem.user_id
    }

}

