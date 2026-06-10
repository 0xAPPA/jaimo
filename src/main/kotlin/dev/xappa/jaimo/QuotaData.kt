package dev.xappa.jaimo

import java.time.LocalDate

data class QuotaInfo(
    val current: Long,
    val maximum: Long,
    val timestamp: String,
)

data class RefillInfo(
    val date: LocalDate,
)

data class QuotaState(
    val quota: QuotaInfo? = null,
    val refill: RefillInfo? = null,
    val error: String? = null,
)

data class MeasurementState(
    val startTokens: Long? = null,
    val startTimestamp: String? = null,
    val endTokens: Long? = null,
    val endTimestamp: String? = null,
)
