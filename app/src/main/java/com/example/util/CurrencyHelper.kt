package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyHelper {

    private val indonesianLocale = Locale.forLanguageTag("id-ID")
    private val rupiahSymbols = DecimalFormatSymbols(indonesianLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val formatter = DecimalFormat("#,###", rupiahSymbols)

    fun formatRupiah(amount: Double, withPrefix: Boolean = true): String {
        val longAmount = amount.toLong()
        val formattedAbs = formatter.format(kotlin.math.abs(longAmount))
        val sign = if (longAmount < 0L) "-" else ""
        return if (withPrefix) "${sign}Rp$formattedAbs" else "$sign$formattedAbs"
    }

    fun formatCompact(amount: Double): String {
        val absAmount = Math.abs(amount)
        return when {
            absAmount >= 1_000_000_000 -> String.format(Locale.US, "%.1fMld", amount / 1_000_000_000)
            absAmount >= 1_000_000 -> {
                val formatted = String.format(Locale.US, "%.1fM", amount / 1_000_000)
                formatted.replace(".0M", "M")
            }
            absAmount >= 1_000 -> {
                val formatted = String.format(Locale.US, "%.0fK", amount / 1_000)
                formatted
            }
            else -> amount.toLong().toString()
        }
    }

    fun formatCompactRupiah(amount: Double): String {
        return if (amount < 0) "-Rp${formatCompact(-amount)}" else "Rp${formatCompact(amount)}"
    }

    fun formatCalendarExpense(amount: Double): String {
        if (amount <= 0) return ""
        val thousands = (amount / 1000).toInt()
        return if (amount % 1000 != 0.0 && amount < 100000) {
            String.format(Locale.US, "-%.1fK", amount / 1000)
        } else {
            "-$thousands" + "K"
        }
    }
}
