package com.budging.app.domain

import java.time.LocalDate
import java.time.ZoneId

interface AppClock {
    fun today(): LocalDate
    fun nowMillis(): Long
    fun zoneId(): ZoneId

    fun toEpochMillis(date: LocalDate): Long {
        return if (date == today()) {
            nowMillis()
        } else {
            date.atStartOfDay(zoneId()).toInstant().toEpochMilli()
        }
    }

    companion object System : AppClock {
        override fun today() = LocalDate.now()
        override fun nowMillis() = java.lang.System.currentTimeMillis()
        override fun zoneId() = ZoneId.systemDefault()
    }
}
