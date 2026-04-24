package com.pengxh.daily.app.utils

import android.content.Context
import android.provider.CalendarContract
import java.util.Calendar

/**
 * 判断是否为工作日（排除法定节假日）
 * 通过读取手机系统日历来判断是否为节假日
 */
object HolidayChecker {

    /**
     * 判断指定日期是否为工作日
     * @param context 上下文
     * @param calendar 要检查的日期
     * @return true=工作日，false=节假日或周末
     */
    fun isWorkingDay(context: Context, calendar: Calendar): Boolean {
        // 先检查是否为周末（周六周日默认休息）
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            // 周末，检查日历中是否有调休上班标记
            if (hasWorkdayMark(context, calendar)) {
                return true
            }
            return false
        }

        // 工作日，检查是否为节假日
        if (isHolidayFromCalendar(context, calendar)) {
            // 节假日，检查是否有调休上班标记
            return hasWorkdayMark(context, calendar)
        }

        return true
    }

    /**
     * 从日历中检查是否有节假日标记
     */
    private fun isHolidayFromCalendar(context: Context, calendar: Calendar): Boolean {
        val contentResolver = context.contentResolver
        val startOfDay = getStartOfDay(calendar)
        val endOfDay = getEndOfDay(calendar)

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.ALL_DAY
        )

        // 查询全天事件（节假日通常是全天事件）
        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTEND} <= ?) AND (${CalendarContract.Events.ALL_DAY} = 1)"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())

        var isHoliday = false

        try {
            val cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val title = it.getString(0) ?: continue

                    if (isHolidayTitle(title)) {
                        isHoliday = true
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return isHoliday
    }

    /**
     * 检查日历中是否有调休上班标记（节假日被调整为工作日）
     */
    private fun hasWorkdayMark(context: Context, calendar: Calendar): Boolean {
        val contentResolver = context.contentResolver
        val startOfDay = getStartOfDay(calendar)
        val endOfDay = getEndOfDay(calendar)

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.ALL_DAY
        )

        // 查询全天事件
        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTEND} <= ?) AND (${CalendarContract.Events.ALL_DAY} = 1)"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())

        try {
            val cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val title = it.getString(0) ?: continue

                    // 检查是否包含"班"字（表示调休上班）
                    if (title.contains("班")) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    /**
     * 判断标题是否为节假日
     */
    private fun isHolidayTitle(title: String): Boolean {
        val holidayKeywords = listOf(
            "休息", "假", "节", "假日", "节假日",
            "元旦", "春节", "清明", "劳动", "端午", "中秋", "国庆"
        )
        return holidayKeywords.any { keyword ->
            title.contains(keyword) && !title.contains("班")
        }
    }

    /**
     * 获取日期的开始时间戳（毫秒）
     */
    private fun getStartOfDay(calendar: Calendar): Long {
        val start = calendar.clone() as Calendar
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)
        return start.timeInMillis
    }

    /**
     * 获取日期的结束时间戳（毫秒）
     */
    private fun getEndOfDay(calendar: Calendar): Long {
        val end = calendar.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)
        return end.timeInMillis
    }

    /**
     * 同步检查日历是否可用
     */
    fun hasCalendarPermission(context: Context): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events._ID),
                null,
                null,
                null
            )
            cursor?.close()
            true
        } catch (e: SecurityException) {
            false
        }
    }
}