package com.budging.app.ui.format

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatCurrency(amountMinor: Long, currencyCode: String): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return try {
        formatter.currency = Currency.getInstance(currencyCode.uppercase())
        formatter.format(amountMinor)
    } catch (_: IllegalArgumentException) {
        "$currencyCode $amountMinor"
    }
}
