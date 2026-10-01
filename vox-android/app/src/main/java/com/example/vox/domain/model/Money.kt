package com.example.vox.domain.model

/** Amount in the currency's minor unit (paise, cents) to avoid floating point errors. */
data class Money(
    val amountMinor: Long,
    val currencyCode: String,
) {
    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Currency mismatch" }
        return copy(amountMinor = amountMinor + other.amountMinor)
    }

    operator fun unaryMinus(): Money = copy(amountMinor = -amountMinor)

    companion object {
        fun zero(currencyCode: String) = Money(0, currencyCode)
    }
}
