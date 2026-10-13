package com.eleazar.localhive.ui.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

private fun Long.toNSDate(): NSDate = NSDate.dateWithTimeIntervalSince1970(this / 1000.0)

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatDate(millis: Long, pattern: String): String =
    NSDateFormatter().apply { dateFormat = pattern }.stringFromDate(millis.toNSDate())

actual fun startOfDay(millis: Long): Long =
    (NSCalendar.currentCalendar.startOfDayForDate(millis.toNSDate()).timeIntervalSince1970 * 1000).toLong()
