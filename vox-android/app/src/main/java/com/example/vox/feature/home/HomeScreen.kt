package com.example.vox.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.IconBadge
import com.example.vox.core.designsystem.component.UiStateContent
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.LocalDateFormatter
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.RecurringPaymentRow
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.icon
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.feature.home.components.GroupSummaryCard
import com.example.vox.feature.home.components.SpendingSummaryCard
import com.example.vox.domain.model.Money
import java.time.YearMonth
import java.math.BigDecimal
import java.util.Currency

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onOpenRecurring: () -> Unit,
    onCreateRecurring: () -> Unit,
    onOpenGroups: () -> Unit,
    onCreateGroup: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingBudget by remember { mutableStateOf(false) }
    var budgetText by remember { mutableStateOf("") }
    var invalidBudget by remember { mutableStateOf(false) }
    val budgetError by viewModel.budgetError.collectAsStateWithLifecycle()
    if (editingBudget) AlertDialog(
        onDismissRequest = { editingBudget = false },
        title = { Text("Monthly budget") },
        text = { Column {
            OutlinedTextField(budgetText, { budgetText = it; invalidBudget = false }, label = { Text("Amount") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            if (invalidBudget) Text("Enter a valid amount", color = MaterialTheme.colorScheme.error)
            budgetError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(onClick = {
            val currency = (state as? UiState.Success)?.data?.summary?.spending?.currencyCode ?: "INR"
            val minor = runCatching { BigDecimal(budgetText).movePointRight(Currency.getInstance(currency).defaultFractionDigits).longValueExact() }.getOrNull()
            if (minor != null && minor >= 0) {
                viewModel.setBudget(minor) { editingBudget = false }
            } else invalidBudget = true
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { editingBudget = false }) { Text("Cancel") } },
    )
    HomeScreen(state, onSetBudget = {
        val model = (state as? UiState.Success)?.data
        val currency = model?.summary?.spending?.currencyCode ?: "INR"
        val digits = Currency.getInstance(currency).defaultFractionDigits
        budgetText = model?.budgetMinor?.let { BigDecimal.valueOf(it, digits).toPlainString() }.orEmpty()
        invalidBudget = false
        editingBudget = true
    }, onCreateRecurring = onCreateRecurring, onCreateGroup = onCreateGroup) { action ->
        when (action) {
            HomeAction.OpenRecurring -> onOpenRecurring()
            HomeAction.OpenGroups -> onOpenGroups()
            is HomeAction.OpenGroup -> onOpenGroup(action.groupId)
            is HomeAction.OpenTransaction -> onOpenTransaction(action.transactionId)
            else -> viewModel.onAction(action)
        }
    }
}

@Composable
fun HomeScreen(state: HomeUiState, onSetBudget: () -> Unit = {}, onCreateRecurring: () -> Unit = {}, onCreateGroup: () -> Unit = {}, onAction: (HomeAction) -> Unit) {
    UiStateContent(state) { model ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
        ) {
            item(key = "header") { HomeHeader(model) }
            item(key = "summary") {
                SpendingSummaryCard(
                    summary = model.summary,
                    month = model.month,
                    budgetMinor = model.budgetMinor,
                    onSetBudget = onSetBudget,
                    showAllCategories = model.showAllCategories,
                    onSelectMode = { onAction(HomeAction.SelectMode(it)) },
                    onToggleBreakdown = { onAction(HomeAction.ToggleBreakdown) },
                    modifier = Modifier.padding(horizontal = VoxSpacing.Medium),
                )
            }

            item(key = "upcoming-header") {
                Column(Modifier.padding(start = 25.dp, end = 20.dp, top = 24.dp)) {
                    Text("ON THE RADAR", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Coming up", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clickable { onAction(HomeAction.OpenRecurring) }.semantics { heading() })
                        IconButton(onClick = onCreateRecurring) { Icon(VoxIcons.Add, contentDescription = "Add recurring payment") }
                    }
                }
            }
            items(model.upcomingPayments, key = { "upcoming-${it.id}" }) { payment ->
                RecurringPaymentRow(
                    payment,
                    Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_open_recurring)) {
                        onAction(HomeAction.OpenRecurring)
                    },
                )
            }

            item(key = "groups-header") {
                Column(Modifier.padding(start = 25.dp, end = 20.dp, top = 24.dp)) {
                    Text("BETTER TOGETHER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Your circles", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clickable { onAction(HomeAction.OpenGroups) }.semantics { heading() })
                        IconButton(onClick = onCreateGroup) { Icon(VoxIcons.Add, contentDescription = "Add group") }
                    }
                }
            }
            item(key = "group-balances") {
                val currency = model.summary.spending.currencyCode
                val money = LocalMoneyFormatter.current
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("YOU OWE", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelMedium)
                            Text(money.format(Money(model.youOweMinor, currency)), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("OWED TO YOU", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
                            Text(money.format(Money(model.owedToYouMinor, currency)), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
            item(key = "groups") {
                Column(Modifier.padding(horizontal = 20.dp).background(MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(17.dp)).padding(horizontal = 12.dp)) {
                    model.groups.forEach { group ->
                        Row(Modifier.fillMaxWidth().height(63.dp).clickable { onAction(HomeAction.OpenGroup(group.id)) },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconBadge(group.type.icon, size = 36.dp)
                            Column(Modifier.weight(1f)) {
                                Text(group.name, style = MaterialTheme.typography.bodyMedium)
                                Text(LocalMoneyFormatter.current.formatSigned(group.balance), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(VoxIcons.Forward, contentDescription = null)
                        }
                    }
                    if (model.groups.isEmpty()) Text("No groups yet", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

        }
    }
}

@Composable
private fun HomeHeader(model: HomeUiModel) {
    Column(Modifier.padding(horizontal = 27.dp, vertical = 20.dp)) {
        Text("YOUR FINANCIAL PULSE  ● LIVE", color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall)
        val greeting = stringResource(model.greetingRes)
        Text(
            if (model.userName.isBlank()) greeting else stringResource(R.string.greeting_named, greeting, model.userName),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
        Text("Here's how your money is moving.", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MonthSelector(month: YearMonth, canGoNext: Boolean, onAction: (HomeAction) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onAction(HomeAction.PreviousMonth) }) {
            Icon(VoxIcons.PreviousMonth, contentDescription = stringResource(R.string.cd_previous_month))
        }
        Text(LocalDateFormatter.current.month(month), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onAction(HomeAction.NextMonth) }, enabled = canGoNext) {
            Icon(VoxIcons.NextMonth, contentDescription = stringResource(R.string.cd_next_month))
        }
    }
}

@Composable
private fun NewGroupCard(onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.width(VoxSize.GroupCardWidth).fillMaxHeight()) {
        Column(
            Modifier.fillMaxWidth().padding(VoxSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VoxSpacing.Small),
        ) {
            Icon(VoxIcons.Add, contentDescription = null)
            Text(stringResource(R.string.create_group), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    VoxTheme { HomeScreen(UiState.Loading) {} }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    VoxTheme {
        HomeScreen(
            UiState.Success(
                HomeUiModel(
                    greetingRes = R.string.greeting_morning,
                    userName = "Aditi",
                    month = YearMonth.now(),
                    canGoToNextMonth = false,
                    summary = SpendingSummaryUi(
                        SummaryMode.SPENDING, PreviewFixtures.inr(18_420), PreviewFixtures.inr(105_000), PreviewFixtures.inr(18_420),
                        -12, emptyList(), emptyList(),
                    ),
                    showAllCategories = false,
                    upcomingPayments = emptyList(),
                    groups = listOf(GroupUi("1", "Goa Trip", PreviewFixtures.group.type, PreviewFixtures.inr(2_450))),
                ),
            ),
        ) {}
    }
}
