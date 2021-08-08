package com.example.plannerapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.plannerapp.data.FriendType
import com.example.plannerapp.databinding.ListFriendBinding

class FriendAdapter(
) : ListAdapter<FriendType, FriendAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private var binding: ListFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.apply {


            }
        }

        fun bind(friendType: FriendType) = with(binding) {
            listFriendName.text = friendType.nameFriend
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

    override fun onBindViewHolder(holder: FriendAdapter.ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<FriendType>() {
            override fun areItemsTheSame(oldItem: FriendType, newItem: FriendType): Boolean {
                return oldItem.idFriend == newItem.idFriend
            }

            override fun areContentsTheSame(oldItem: FriendType, newItem: FriendType): Boolean {
                return oldItem == newItem
            }
        }
    }

}

