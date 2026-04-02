package com.conboi.features.main.task.dialog

//
// @HiltViewModel
// class ReminderDialogViewModel
//    @Inject
//    constructor() : ViewModel() {
//        private val _bufferCalendar = MutableLiveData<Calendar>()
//        val bufferCalendar: LiveData<Calendar> = _bufferCalendar
//
//        private val _bufferTime = MutableLiveData(GLOBAL_START_DATE)
//        val bufferTime: LiveData<Long> = _bufferTime
//
//        private val _bufferRepeatMode = MutableLiveData(RepeatMode.Once)
//        val bufferRepeatMode: LiveData<RepeatMode> = _bufferRepeatMode
//
//        fun updateBufferCalendar(calendar: Calendar) {
//            _bufferCalendar.value = calendar
//        }
//
//        fun updateBufferTime(time: Long) {
//            _bufferTime.value = time
//        }
//
//        fun updateBufferRepeatMode(repeatMode: RepeatMode) {
//            _bufferRepeatMode.value = repeatMode
//        }
//    }