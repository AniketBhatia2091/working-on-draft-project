package com.daykhata.app.domain.model

data class Settings(
    val budget: Int = 15000,
    val cutOffLimit: Int = 2000,
    val periodType: PeriodType = PeriodType.MONTHLY,
    val periodStartDate: Long = System.currentTimeMillis()
)
