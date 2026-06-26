package com.budging.app.ui.format

import java.text.NumberFormat
import java.util.Locale

private val idrFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

fun formatIdr(amountMinor: Long): String = idrFormatter.format(amountMinor)
