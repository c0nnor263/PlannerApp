package com.conboi.plannerapp.interfaces

interface UpdateTotalTaskCallback {
    fun onIncrement(differ: Int)
    fun onDecrement(differ: Int)
}