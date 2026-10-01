package com.example.vox.core.common

import android.content.Context
import com.example.vox.R
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

interface VoxDateFormatter {
    /** Today / Yesterday / Oct 1 / Oct 1, 2025 */
    fun transactionDate(instant: Instant): String
    fun relativeDate(date: LocalDate): String
    fun time(instant: Instant): String
    /** Oct 5 */
    fun dueDate(date: LocalDate): String
    /** October 1 */
    fun longDate(instant: Instant): String
    /** October 2026 */
    fun month(month: YearMonth): String
}

class AndroidVoxDateFormatter(
    private val context: Context,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val locale: Locale = Locale.getDefault(),
) : VoxDateFormatter {
    private val shortDate = DateTimeFormatter.ofPattern("MMM d", locale)
    private val shortDateYear = DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
    private val longDate = DateTimeFormatter.ofPattern("MMMM d", locale)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

    override fun transactionDate(instant: Instant): String = relativeDate(instant.atZone(zone).toLocalDate())

    override fun relativeDate(date: LocalDate): String {
        val today = LocalDate.now(zone)
        return when (date) {
            today -> context.getString(R.string.date_today)
            today.minusDays(1) -> context.getString(R.string.date_yesterday)
            else -> date.format(if (date.year == today.year) shortDate else shortDateYear)
        }
    }

    override fun time(instant: Instant): String = instant.atZone(zone).format(timeFormat)

    override fun dueDate(date: LocalDate): String = date.format(shortDate)

    override fun longDate(instant: Instant): String = instant.atZone(zone).format(longDate)

    override fun month(month: YearMonth): String = month.format(monthYear)
}
