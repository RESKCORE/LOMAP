package com.example.utils

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val wholeFormat = DecimalFormat("#,##,##0")
    private val decimalFormat = DecimalFormat("#,##,##0.0")

    fun formatInr(amount: Double?, symbol: String = "₹"): String {
        if (amount == null || amount.isNaN() || amount.isInfinite()) return "Unknown"
        return if (amount % 1.0 == 0.0) {
            "$symbol${wholeFormat.format(amount.toLong())}"
        } else {
            "$symbol${decimalFormat.format(amount)}"
        }
    }

    fun formatDistanceKm(distanceKm: Double?): String {
        if (distanceKm == null || distanceKm.isNaN()) return "Unavailable"
        return "${decimalFormat.format(distanceKm)} km"
    }

    fun formatRatePerKm(rateInr: Double?, symbol: String = "₹"): String {
        if (rateInr == null || rateInr.isNaN()) return "N/A"
        return "$symbol${decimalFormat.format(rateInr)}/km"
    }

    fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.US)
        return sdf.format(Date(millis))
    }

    fun formatShortTime(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        return sdf.format(Date(millis))
    }

    fun formatDurationSeconds(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    }
}
