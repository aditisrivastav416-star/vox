package com.example.vox.domain.model

import java.time.LocalDate

enum class Frequency { DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM }

data class RecurringPayment(
    val id: String,
    val name: String,
    val category: Category,
    /** Null means "ask me every time". */
    val amount: Money?,
    val frequency: Frequency,
    /** Only used when [frequency] is [Frequency.CUSTOM]. */
    val customIntervalDays: Int? = null,
    val nextDueDate: LocalDate,
    val remindDaysBefore: Boolean = true,
    val remindOnDueDate: Boolean = true,
)
