package com.example.vox.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.BalanceText
import com.example.vox.core.designsystem.component.EmptyState
import com.example.vox.core.designsystem.component.SectionHeader
import com.example.vox.core.designsystem.component.UiStateContent
import com.example.vox.core.designsystem.component.VoxTopBar
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.displayName

@Composable
fun GroupDetailRoute(viewModel: GroupDetailViewModel, onBack: () -> Unit, onAddExpense: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { if (it == GroupDetailEvent.Deleted) onBack() }
    GroupDetailScreen(state, onBack) { action ->
        if (action == GroupDetailAction.AddExpense) onAddExpense(viewModel.groupId) else viewModel.onAction(action)
    }
}

@Composable
fun GroupDetailScreen(state: UiState<GroupDetailUiModel>, onBack: () -> Unit, onAction: (GroupDetailAction) -> Unit) {
    val group = (state as? UiState.Success)?.data?.group
    Scaffold(
        topBar = {
            VoxTopBar(group?.name.orEmpty(), onBack = onBack) {
                if (group != null) GroupMenu(onDelete = { onAction(GroupDetailAction.DeleteGroup) })
            }
        },
    ) { padding ->
        UiStateContent(state, Modifier.padding(padding)) { model ->
            if (model.group == null || model.balance == null) {
                EmptyState(stringResource(R.string.group_not_found), stringResource(R.string.group_not_found_body), Modifier.padding(padding))
                return@UiStateContent
            }
            val money = LocalMoneyFormatter.current
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = VoxSpacing.Medium, end = VoxSpacing.Medium,
                    top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + VoxSpacing.Medium,
                ),
                verticalArrangement = Arrangement.spacedBy(VoxSpacing.Small),
            ) {
                item(key = "balance") { BalanceText(model.balance, amountStyle = MaterialTheme.typography.displaySmall) }
                items(model.memberBalances, key = { "member-${it.person.id}" }) { member ->
                    val amount = money.format(member.balance.copy(amountMinor = kotlin.math.abs(member.balance.amountMinor)))
                    val memberName = member.person.displayName() + if (member.person.isOnVox) "" else " (not on Vox)"
                    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                        Text(
                            if (member.balance.amountMinor > 0) {
                                stringResource(R.string.member_owes_you, memberName)
                            } else {
                                stringResource(R.string.you_owe_member, memberName)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Text(amount)
                    }
                }
                item(key = "actions") {
                    Row(Modifier.padding(vertical = VoxSpacing.Small), horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Small)) {
                        Button(
                            onClick = { onAction(GroupDetailAction.AddExpense) },
                            modifier = Modifier.weight(1f).heightIn(min = VoxSize.MinTouchTarget),
                        ) { Text(stringResource(R.string.group_add_expense)) }
                        OutlinedButton(
                            onClick = { onAction(GroupDetailAction.ShowSettleConfirmation(true)) },
                            enabled = model.memberBalances.isNotEmpty(),
                            modifier = Modifier.weight(1f).heightIn(min = VoxSize.MinTouchTarget),
                        ) { Text(stringResource(R.string.group_settle_up)) }
                    }
                }
                item(key = "expenses-header") { SectionHeader(stringResource(R.string.group_expenses)) }
                if (model.expenses.isEmpty()) {
                    item(key = "no-expenses") { Text(stringResource(R.string.group_no_expenses), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(model.expenses, key = { it.expense.id }) { ExpenseRow(it) }
            }
            if (model.showSettleConfirmation) {
                AlertDialog(
                    onDismissRequest = { onAction(GroupDetailAction.ShowSettleConfirmation(false)) },
                    title = { Text(stringResource(R.string.group_settle_title)) },
                    text = { Text(stringResource(R.string.group_settle_body)) },
                    confirmButton = { TextButton(onClick = { onAction(GroupDetailAction.ConfirmSettle) }) { Text(stringResource(R.string.group_settle_up)) } },
                    dismissButton = {
                        TextButton(onClick = { onAction(GroupDetailAction.ShowSettleConfirmation(false)) }) { Text(stringResource(R.string.cancel)) }
                    },
                )
            }
        }
    }
}

@Composable
private fun GroupMenu(onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(VoxIcons.More, contentDescription = stringResource(R.string.cd_more_options)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.group_delete)) }, onClick = {
                expanded = false
                onDelete()
            })
        }
    }
}

@Composable
private fun ExpenseRow(item: GroupExpenseUi) {
    val money = LocalMoneyFormatter.current
    Column(Modifier.fillMaxWidth().padding(vertical = VoxSpacing.Small).semantics(mergeDescendants = true) {}) {
        Row {
            Text(item.expense.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(money.format(item.expense.total), style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            stringResource(R.string.group_paid_by, item.paidBy?.displayName().orEmpty()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.group_your_share, money.format(item.yourShare)), style = MaterialTheme.typography.bodySmall)
    }
}

@Preview
@Composable
private fun GroupDetailPreview() {
    val group = PreviewFixtures.group
    VoxTheme {
        GroupDetailScreen(
            UiState.Success(
                GroupDetailUiModel(
                    group, group.netBalance, group.memberBalances(),
                    group.expenses.map { GroupExpenseUi(it, group.members.first(), PreviewFixtures.inr(1_200)) },
                    showSettleConfirmation = false,
                ),
            ),
            {}, {},
        )
    }
}
