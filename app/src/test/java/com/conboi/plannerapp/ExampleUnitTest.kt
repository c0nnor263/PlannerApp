package com.conboi.plannerapp

import com.conboi.plannerapp.ui.main.MainFragment
import com.conboi.plannerapp.utils.AlarmType
import com.conboi.plannerapp.utils.getUniqueRequestCode
import org.junit.Before
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    private lateinit var SUT: MainFragment

    @Before
    fun setUp() {
        SUT = MainFragment()
    }

    @Test
    fun checkUniqueRequestCodeWork() {
        val result = getUniqueRequestCode(alarmType = AlarmType.REMINDER, 123) == 1111123
        assert(result)
    }
}
