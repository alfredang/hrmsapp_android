package com.tertiaryinfotech.hrportal.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WorkingDaysResult(
    val workingDays: Int,
    val calendarDays: Int,
    val weekendDays: Int,
    val holidayDays: Int,
)

/** Ports the web's `calculateWorkingDays()` (`tertiary-hrms/src/lib/utils.ts`) exactly: walks
 *  every calendar day from [start] to [end] inclusive, classifying each as a weekend, a public
 *  holiday, or a working day — feeds the Apply-for-Leave working-days preview. */
object WorkingDays {
    private val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun calculate(start: Date, end: Date, holidays: Set<String>): WorkingDaysResult {
        val cursor = dayStart(start)
        val endCal = dayStart(end)

        var workingDays = 0
        var weekendDays = 0
        var holidayDays = 0
        var calendarDays = 0

        while (!cursor.after(endCal)) {
            calendarDays++
            val dow = cursor.get(Calendar.DAY_OF_WEEK) // Calendar.SUNDAY=1, SATURDAY=7
            val iso = isoFmt.format(cursor.time)
            when {
                dow == Calendar.SUNDAY || dow == Calendar.SATURDAY -> weekendDays++
                holidays.contains(iso) -> holidayDays++
                else -> workingDays++
            }
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }

        return WorkingDaysResult(workingDays, calendarDays, weekendDays, holidayDays)
    }

    private fun dayStart(d: Date): Calendar = Calendar.getInstance().apply {
        time = d
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
}
