package com.eleazar.localhive.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun formatDate(millis: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

actual fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
