package com.russert.woodshed.util

object TimeFormatter {
    fun format(seconds: Double): String {
        if (!seconds.isFinite() || seconds.isNaN()) return "0:00"
        val total = seconds.toInt().coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%d:%02d", m, s)
    }

    fun formatDetailed(seconds: Double): String {
        if (!seconds.isFinite() || seconds.isNaN()) return "0:00.0"
        val total = seconds.toInt().coerceAtLeast(0)
        val m = total / 60
        val s = total % 60
        val tenths = ((seconds % 1.0) * 10).toInt()
        return String.format("%d:%02d.%d", m, s, tenths)
    }
}
