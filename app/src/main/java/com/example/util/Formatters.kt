package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
    private val indianSymbols = DecimalFormatSymbols(
        Locale.Builder().setLanguage("en").setRegion("IN").build()
    )

    fun formatRupee(amount: Double, showSign: Boolean = false): String {
        val absVal = abs(amount)
        val formatted = formatIndianNumber(absVal)
        return when {
            showSign && amount > 0 -> "+₹$formatted"
            showSign && amount < 0 -> "-₹$formatted"
            amount < 0 -> "-₹$formatted"
            else -> "₹$formatted"
        }
    }

    fun formatIndianNumber(value: Double): String {
        val totalPaise = kotlin.math.round(abs(value) * 100.0).toLong()
        val whole = totalPaise / 100
        val fraction = (totalPaise % 100).toInt()
        val wholeStr = formatIndianInteger(whole)
        return if (fraction > 0) {
            "$wholeStr.${fraction.toString().padStart(2, '0')}"
        } else {
            wholeStr
        }
    }

    private fun formatIndianInteger(number: Long): String {
        val str = abs(number).toString()
        if (str.length <= 3) return str
        val lastThree = str.takeLast(3)
        val rest = str.dropLast(3)
        val sb = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            sb.append(rest[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(',')
            }
        }
        return "${sb.reverse()},$lastThree"
    }

    fun formatDate(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(millis))

    fun formatDateTime(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(millis))

    fun formatTime(millis: Long): String =
        SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(millis))

    fun formatShortDay(millis: Long): String =
        SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date(millis))

    fun startOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun daysAgoMillis(days: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun formatCompactRupee(amount: Double): String {
        val absVal = abs(amount)
        val df = DecimalFormat("#.#", indianSymbols)
        return when {
            absVal >= 1_00_00_000 -> "₹${df.format(absVal / 1_00_00_000)}Cr"
            absVal >= 1_00_000 -> "₹${df.format(absVal / 1_00_000)}L"
            absVal >= 1_000 -> "₹${df.format(absVal / 1_000)}K"
            else -> formatRupee(absVal)
        }
    }
}
