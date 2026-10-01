package com.example.vox.feature.activity

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.EmptyState
import com.example.vox.core.designsystem.component.UiStateContent
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.LocalDateFormatter
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.TransactionRow
import com.example.vox.core.ui.UiState
import java.time.LocalDate
import java.time.YearMonth
import com.example.vox.domain.model.TransactionType

@Composable
fun ActivityRoute(viewModel: ActivityViewModel, onOpenTransaction: (String) -> Unit, onAddTransaction: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ActivityScreen(state) { action ->
        when (action) {
            is ActivityAction.OpenTransaction -> onOpenTransaction(action.id)
            ActivityAction.AddTransaction -> onAddTransaction()
            else -> viewModel.onAction(action)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityScreen(state: UiState<ActivityUiModel>, onAction: (ActivityAction) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 23.dp).padding(top = 20.dp, bottom = 14.dp)) {
            Text("THE WHOLE PICTURE", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp,
                letterSpacing = 1.5.sp)
            Text("Your activity ✳", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 5.dp).semantics { heading() })
            Text("Every little thing, all in one place.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        UiStateContent(state) { model ->
            val month = YearMonth.now()
            val expenses = model.days.filter { YearMonth.from(it.date) == month }.flatMap { it.items }
                .map { it.transaction }.filter { it.type == TransactionType.EXPENSE }
            val total = expenses.sumOf { it.amount.amountMinor }
            val currency = expenses.firstOrNull()?.amount?.currencyCode ?: "INR"
            Column(Modifier.fillMaxWidth().padding(horizontal = 23.dp).background(
                Brush.linearGradient(listOf(Color(0xFF302872), Color(0xFF5554AF), Color(0xFF8BA6EE))),
                RoundedCornerShape(17.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("SPENT THIS MONTH", color = Color.White.copy(alpha = .8f), fontSize = 10.sp, letterSpacing = 1.3.sp)
                Text(LocalMoneyFormatter.current.format(com.example.vox.domain.model.Money(total, currency)),
                    color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("${expenses.size} transactions tracked", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
            }
            if (model.hasNoTransactions) {
                EmptyState(
                    title = stringResource(R.string.activity_empty_title),
                    body = stringResource(R.string.activity_empty_body),
                    actionLabel = stringResource(R.string.add_transaction),
                    onAction = { onAction(ActivityAction.AddTransaction) },
                )
                return@UiStateContent
            }
            OutlinedTextField(
                value = model.query,
                onValueChange = { onAction(ActivityAction.QueryChanged(it)) },
                placeholder = { Text(stringResource(R.string.search_transactions)) },
                leadingIcon = { Icon(VoxIcons.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp, vertical = 12.dp).height(55.dp),
            )
            if (model.days.isEmpty()) {
                EmptyState(title = stringResource(R.string.activity_no_results_title), body = stringResource(R.string.activity_no_results_body))
                return@UiStateContent
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)) {
                model.days.forEach { day ->
                    stickyHeader(key = day.date.toString()) { DayHeader(day.date) }
                    items(day.items, key = { it.transaction.id }) { item ->
                        TransactionRow(
                            transaction = item.transaction,
                            groupName = item.groupName,
                            onClick = { onAction(ActivityAction.OpenTransaction(item.transaction.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: ActivityFilter, onSelect: (ActivityFilter) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = VoxSpacing.Medium, vertical = VoxSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Small),
    ) {
        ActivityFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        stringResource(
                            when (filter) {
                                ActivityFilter.ALL -> R.string.filter_all
                                ActivityFilter.EXPENSES -> R.string.filter_expenses
                                ActivityFilter.INCOME -> R.string.filter_income
                                ActivityFilter.GROUPS -> R.string.filter_groups
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate) {
    Surface(Modifier.fillMaxWidth()) {
        Text(
            LocalDateFormatter.current.relativeDate(date).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = VoxSpacing.Medium, vertical = VoxSpacing.Small).semantics { heading() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityScreenPreview() {
    VoxTheme {
        ActivityScreen(
            UiState.Success(
                ActivityUiModel(
                    query = "", filter = ActivityFilter.ALL, hasNoTransactions = false,
                    days = listOf(
                        ActivityDayUi(
                            LocalDate.now(),
                            listOf(ActivityItemUi(PreviewFixtures.expense, null), ActivityItemUi(PreviewFixtures.income, null)),
                        ),
                    ),
                ),
            ),
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityScreenEmptyPreview() {
    VoxTheme { ActivityScreen(UiState.Success(ActivityUiModel("", ActivityFilter.ALL, emptyList(), true))) {} }
}
