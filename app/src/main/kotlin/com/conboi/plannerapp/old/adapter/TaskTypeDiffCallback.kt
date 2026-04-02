package com.conboi.plannerapp.old.adapter

//
// class TaskTypeDiffCallback
//    @Inject
//    constructor() : DiffUtil.ItemCallback<TaskType>() {
//        override fun areItemsTheSame(
//            oldItem: TaskType,
//            newItem: TaskType
//        ): Boolean = oldItem.idTask == newItem.idTask
//
//        override fun areContentsTheSame(
//            oldItem: TaskType,
//            newItem: TaskType
//        ): Boolean = oldItem == newItem
//
//        override fun getChangePayload(
//            oldItem: TaskType,
//            newItem: TaskType
//        ): Any? =
//            if (oldItem.title != newItem.title) {
//                newItem.title
//            } else {
//                super.getChangePayload(
//                    oldItem,
//                    newItem
//                )
//            }
//    }