package com.example.vox.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.stateInUi
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.repository.GroupRepository
import com.example.vox.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

enum class ActivityFilter { ALL, EXPENSES, INCOME, GROUPS }

data class ActivityItemUi(val transaction: Transaction, val groupName: String?)

data class ActivityDayUi(val date: LocalDate, val items: List<ActivityItemUi>)

data class ActivityUiModel(
    val query: String,
    val filter: ActivityFilter,
    val days: List<ActivityDayUi>,
    /** True when the user has no transactions at all (vs. nothing matching the search). */
    val hasNoTransactions: Boolean,
)

sealed interface ActivityAction {
    data class QueryChanged(val query: String) : ActivityAction
    data class FilterChanged(val filter: ActivityFilter) : ActivityAction
    data class OpenTransaction(val id: String) : ActivityAction
    data object AddTransaction : ActivityAction
}

class ActivityViewModel(
    transactionRepository: TransactionRepository,
    groupRepository: GroupRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ActivityFilter.ALL)

    val state: StateFlow<UiState<ActivityUiModel>> = combine(
        transactionRepository.observeTransactions(),
        groupRepository.observeGroups(),
        query,
        filter,
    ) { transactions, groups, q, f ->
        val groupNames = groups.associate { it.id to it.name }
        val zone = ZoneId.systemDefault()
        val days = transactions
            .filter { it.matches(f) && it.matches(q, groupNames) }
            .groupBy { it.timestamp.atZone(zone).toLocalDate() }
            .map { (date, list) -> ActivityDayUi(date, list.map { ActivityItemUi(it, it.groupId?.let(groupNames::get)) }) }
        ActivityUiModel(q, f, days, hasNoTransactions = transactions.isEmpty())
    }.stateInUi(viewModelScope)

    fun onAction(action: ActivityAction) {
        when (action) {
            is ActivityAction.QueryChanged -> query.value = action.query
            is ActivityAction.FilterChanged -> filter.value = action.filter
            is ActivityAction.OpenTransaction, ActivityAction.AddTransaction -> Unit // navigation
        }
    }
}

private fun Transaction.matches(filter: ActivityFilter) = when (filter) {
    ActivityFilter.ALL -> true
    ActivityFilter.EXPENSES -> type == TransactionType.EXPENSE
    ActivityFilter.INCOME -> type == TransactionType.INCOME
    ActivityFilter.GROUPS -> groupId != null
}

/** ponytail: plain substring match on merchant/notes/category/method; natural-language search plugs in here later. */
private fun Transaction.matches(query: String, groupNames: Map<String, String>): Boolean {
    if (query.isBlank()) return true
    val haystack = listOfNotNull(title, notes, category?.name, paymentMethod.name, groupId?.let(groupNames::get))
    return haystack.any { it.contains(query.trim(), ignoreCase = true) }
}
