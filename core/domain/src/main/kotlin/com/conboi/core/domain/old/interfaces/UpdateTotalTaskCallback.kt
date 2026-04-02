package com.conboi.core.domain.old.interfaces

interface UpdateTotalTaskCallback {
    fun onIncrement(differ: Int)

    fun onDecrement(differ: Int)
}