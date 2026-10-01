package com.example.vox.core.common

import com.example.vox.domain.model.Money
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

/** The single place money becomes text (and text becomes money). */
class MoneyFormatter(private val locale: Locale = Locale.getDefault()) {

    /** ₹18,000 or ₹508.75 — decimals only when there are any. */
    fun format(money: Money): String {
        val currency = Currency.getInstance(money.currencyCode)
        val digits = currency.defaultFractionDigits.coerceAtLeast(0)
        val factor = BigDecimal.TEN.pow(digits)
        val value = BigDecimal.valueOf(abs(money.amountMinor)).divide(factor)
        val showFraction = abs(money.amountMinor) % factor.toLong() != 0L
        val format = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            minimumFractionDigits = if (showFraction) digits else 0
            maximumFractionDigits = if (showFraction) digits else 0
        }
        val sign = if (money.amountMinor < 0) "-" else ""
        return sign + format.format(value)
    }

    /** +₹2,450 / -₹740. */
    fun formatSigned(money: Money): String =
        if (money.amountMinor > 0) "+" + format(money) else format(money)

    /** Plain number for editable fields, e.g. "400" or "508.75". */
    fun formatInput(money: Money): String {
        val digits = Currency.getInstance(money.currencyCode).defaultFractionDigits.coerceAtLeast(0)
        return BigDecimal.valueOf(money.amountMinor).movePointLeft(digits).stripTrailingZeros().toPlainString()
    }

    /** Parses user input like "400" or "1,250.50". Returns null when invalid or not positive. */
    fun parse(input: String, currencyCode: String): Money? {
        val digits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
        val decimal = input.replace(",", "").trim().toBigDecimalOrNull() ?: return null
        if (decimal.signum() <= 0 || decimal.scale() > digits) return null
        return runCatching { Money(decimal.movePointRight(digits).longValueExact(), currencyCode) }.getOrNull()
    }
}
