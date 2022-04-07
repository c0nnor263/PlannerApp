package com.conboi.plannerapp.ui.main.task.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.conboi.plannerapp.utils.GLOBAL_START_DATE
import com.conboi.plannerapp.utils.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReminderDialogViewModel @Inject constructor() : ViewModel() {
    private val _bufferCalendar = MutableLiveData<Calendar>()
    val bufferCalendar: LiveData<Calendar> = _bufferCalendar

    private val _bufferTime = MutableLiveData(GLOBAL_START_DATE)
    val bufferTime: LiveData<Long> = _bufferTime

    private val _bufferRepeatMode = MutableLiveData(RepeatMode.Once)
    val bufferRepeatMode: LiveData<RepeatMode> = _bufferRepeatMode


    fun updateBufferCalendar(calendar: Calendar) {
        _bufferCalendar.value = calendar
    }

    fun updateBufferTime(time: Long) {
        _bufferTime.value = time
    }

    fun updateBufferRepeatMode(repeatMode: RepeatMode) {
        _bufferRepeatMode.value = repeatMode
    }
}