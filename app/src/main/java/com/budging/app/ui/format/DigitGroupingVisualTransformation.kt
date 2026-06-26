package com.budging.app.ui.format

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object DigitGroupingVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val grouped = raw.reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()

        val commasBeforeOffset = IntArray(raw.length + 1)
        var rawIndex = 0
        var commaCount = 0
        grouped.forEach { char ->
            if (char == ',') {
                commaCount += 1
            } else {
                rawIndex += 1
                commasBeforeOffset[rawIndex] = commaCount
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, raw.length)
                return safeOffset + commasBeforeOffset[safeOffset]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, grouped.length)
                return grouped.take(safeOffset).count { it != ',' }.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(grouped), offsetMapping)
    }
}
