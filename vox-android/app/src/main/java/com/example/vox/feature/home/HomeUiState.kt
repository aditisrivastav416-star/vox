package com.example.vox.feature.home

import androidx.annotation.StringRes
import com.example.vox.core.ui.UiState
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.GroupType
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.RecurringPayment
import java.time.YearMonth

typealias HomeUiState = UiState<HomeUiModel>

enum class SummaryMode { SPENDING, INCOME, NET }

data class CategoryAmountUi(
    val category: Category,
    val amount: Money,
    /** Share of the mode total, 0..1, for the bar. */
    val fraction: Float,
)

/** One column of the chart. [weekNumber] is 1-based within the month; [value] may be negative in NET mode. */
data class ChartBarUi(
    val weekNumber: Int,
    val value: Money,
)

data class SpendingSummaryUi(
    val mode: SummaryMode,
    val total: Money,
    val income: Money,
    val spending: Money,
    /** Percent change of [total] vs. previous month; null when not meaningful. */
    val changePercent: Int?,
    val bars: List<ChartBarUi>,
    val categories: List<CategoryAmountUi>,
)

data class GroupUi(
    val id: String,
    val name: String,
    val type: GroupType,
    val balance: Money,
)

data class HomeUiModel(
    @param:StringRes val greetingRes: Int,
    val userName: String,
    val month: YearMonth,
    val canGoToNextMonth: Boolean,
    val summary: SpendingSummaryUi,
    val budgetMinor: Long? = null,
    val showAllCategories: Boolean,
    val upcomingPayments: List<RecurringPayment>,
    val groups: List<GroupUi>,
)
