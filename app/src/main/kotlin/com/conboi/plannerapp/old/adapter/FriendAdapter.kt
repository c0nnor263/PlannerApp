package com.conboi.plannerapp.old.adapter

//
// class FriendAdapter(
//    options: FirestorePagingOptions<FriendType>,
//    private val listener: FriendListInterface
// ) : FirestorePagingAdapter<FriendType, com.conboi.plannerapp.old.adapter.FriendAdapter.ViewHolder>(options) {
//    inner class ViewHolder(private var binding: ListFriendBinding) :
//        RecyclerView.ViewHolder(binding.root) {
//        init {
//            binding.root.setOnClickListener {
//                val position = bindingAdapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    val friend = getItem(position)
//                    listener.onClick(
//                        friend!!.toObject<FriendType>()!!
//                    )
//                }
//            }
//
//            binding.root.setOnLongClickListener {
//                val position = bindingAdapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    val friend = getItem(position)!!.toObject<FriendType>()!!
//                    listener.onHold(
//                        binding.ivAvatar,
//                        friend.user_id
//                    )
//                }
//                true
//            }
//        }
//
//        fun bind(friend: FriendType) {
//            binding.friend = friend
//            binding.executePendingBindings()
//        }
//    }
//
//    override fun onCreateViewHolder(
//        parent: ViewGroup,
//        viewType: Int
//    ): _root_ide_package_.com.conboi.plannerapp.old.adapter.FriendAdapter.ViewHolder =
//        ViewHolder(
//            ListFriendBinding.inflate(
//                LayoutInflater.from(parent.context),
//                parent,
//                false
//            )
//        )
//
//    override fun onBindViewHolder(
//        holder: _root_ide_package_.com.conboi.plannerapp.old.adapter.FriendAdapter.ViewHolder,
//        position: Int,
//        model: FriendType
//    ) = holder.bind(model)
// }