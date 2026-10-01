package com.example.vox.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.R
import com.example.vox.core.ui.stateInUi
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.repository.GroupRepository
import com.example.vox.domain.repository.RecurringPaymentRepository
import com.example.vox.domain.repository.TransactionRepository
import com.example.vox.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

private const val MAX_UPCOMING = 3
private const val COLLAPSED_CATEGORIES = 4
private const val DEFAULT_CURRENCY = "INR"

private data class HomeControls(
    val month: YearMonth = YearMonth.now(),
    val mode: SummaryMode = SummaryMode.SPENDING,
    val showAllCategories: Boolean = false,
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    groupRepository: GroupRepository,
    recurringPaymentRepository: RecurringPaymentRepository,
    userRepository: UserRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(HomeControls())

    val state: StateFlow<HomeUiState> = combine(
        transactionRepository.observeTransactions(),
        groupRepository.observeGroups(),
        recurringPaymentRepository.observePayments(),
        userRepository.observeProfile(),
        controls,
    ) { transactions, groups, payments, profile, c ->
        val currency = profile?.currencyCode ?: DEFAULT_CURRENCY
        val summary = buildSummary(transactions, c.month, c.mode, currency, ZoneId.systemDefault())
        HomeUiModel(
            greetingRes = greetingFor(LocalTime.now()),
            userName = profile?.name.orEmpty(),
            month = c.month,
            canGoToNextMonth = c.month < YearMonth.now(),
            summary = if (c.showAllCategories) summary else summary.copy(categories = summary.categories.take(COLLAPSED_CATEGORIES)),
            showAllCategories = c.showAllCategories,
            upcomingPayments = payments.take(MAX_UPCOMING),
            groups = groups.map { GroupUi(it.id, it.name, it.type, it.netBalance) },
        )
    }.stateInUi(viewModelScope)

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.PreviousMonth -> controls.update { it.copy(month = it.month.minusMonths(1)) }
            HomeAction.NextMonth -> controls.update { c ->
                if (c.month < YearMonth.now()) c.copy(month = c.month.plusMonths(1)) else c
            }
            is HomeAction.SelectMode -> controls.update { it.copy(mode = action.mode) }
            HomeAction.ToggleBreakdown -> controls.update { it.copy(showAllCategories = !it.showAllCategories) }
            HomeAction.OpenRecurring, HomeAction.OpenGroups, HomeAction.CreateGroup,
            is HomeAction.OpenGroup, is HomeAction.OpenTransaction -> Unit // navigation, handled by the route
        }
    }
}

private fun greetingFor(time: LocalTime) = when {
    time.hour < 12 -> R.string.greeting_morning
    time.hour < 17 -> R.string.greeting_afternoon
    else -> R.string.greeting_evening
}

/**
 * Monthly totals for one currency.
 * ponytail: transactions in other currencies are ignored; add FX conversion when multi-currency wallets exist.
 */
internal fun buildSummary(
    transactions: List<Transaction>,
    month: YearMonth,
    mode: SummaryMode,
    currency: String,
    zone: ZoneId,
): SpendingSummaryUi {
    fun Transaction.month() = YearMonth.from(timestamp.atZone(zone))
    fun Transaction.signedFor(mode: SummaryMode): Long = when (mode) {
        SummaryMode.SPENDING -> if (type == TransactionType.EXPENSE) amount.amountMinor else 0
        SummaryMode.INCOME -> if (type == TransactionType.INCOME) amount.amountMinor else 0
        SummaryMode.NET -> if (type == TransactionType.INCOME) amount.amountMinor else -amount.amountMinor
    }

    val sameCurrency = transactions.filter { it.amount.currencyCode == currency }
    val thisMonth = sameCurrency.filter { it.month() == month }
    val lastMonth = sameCurrency.filter { it.month() == month.minusMonths(1) }

    val total = thisMonth.sumOf { it.signedFor(mode) }
    val previous = lastMonth.sumOf { it.signedFor(mode) }
    val change = if (mode != SummaryMode.NET && previous > 0) ((total - previous) * 100.0 / previous).roundToInt() else null

    val weeks = (month.lengthOfMonth() + 6) / 7
    val bars = (1..weeks).map { week ->
        val value = thisMonth
            .filter { (it.timestamp.atZone(zone).dayOfMonth - 1) / 7 + 1 == week }
            .sumOf { it.signedFor(mode) }
        ChartBarUi(week, Money(value, currency))
    }

    val categoryType = if (mode == SummaryMode.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
    val categories = if (mode == SummaryMode.NET) emptyList() else {
        val byCategory = thisMonth
            .filter { it.type == categoryType }
            .groupBy { it.category ?: Category.OTHER }
            .mapValues { (_, list) -> list.sumOf { it.amount.amountMinor } }
        byCategory.entries
            .sortedByDescending { it.value }
            .map { (category, amount) ->
                CategoryAmountUi(category, Money(amount, currency), if (total > 0) amount.toFloat() / total else 0f)
            }
    }

    return SpendingSummaryUi(
        mode = mode,
        total = Money(total, currency),
        income = Money(thisMonth.sumOf { it.signedFor(SummaryMode.INCOME) }, currency),
        spending = Money(thisMonth.sumOf { it.signedFor(SummaryMode.SPENDING) }, currency),
        changePercent = change,
        bars = bars,
        categories = categories,
    )
}
