package com.conboi.plannerapp.ui.main.task.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DeadlineDialogViewModel @Inject constructor() : ViewModel() {
    private val _bufferCalendar = MutableLiveData<Calendar>()
    val bufferCalendar: LiveData<Calendar> = _bufferCalendar

    private val _bufferMissed = MutableLiveData(false)
    val bufferMissed: LiveData<Boolean> = _bufferMissed

    private val _bufferDeadline = MutableLiveData(GLOBAL_START_DATE)
    val bufferDeadline: LiveData<Long> = _bufferDeadline


    fun updateBufferCalendar(calendar: Calendar) {
        _bufferCalendar.value = calendar
    }

    fun updateBufferMissed(missed: Boolean) {
        _bufferMissed.value = missed
    }

    fun updateBufferDeadline(time: Long) {
        _bufferDeadline.value = time
    }
}