package com.example.vox.feature.split

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.CheckboxRow
import com.example.vox.core.designsystem.component.IconBadge
import com.example.vox.core.designsystem.component.LoadingState
import com.example.vox.core.designsystem.component.VoxTopBar
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.displayName
import com.example.vox.domain.model.CURRENT_USER_ID
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.Person
import java.util.Currency

@Composable
fun SplitRoute(viewModel: SplitViewModel, onExit: () -> Unit, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) {
        when (it) {
            SplitEvent.Done -> onDone()
            SplitEvent.Exit -> onExit()
        }
    }
    SplitScreen(state, viewModel::onAction)
}

@Composable
fun SplitScreen(state: SplitUiState, onAction: (SplitAction) -> Unit) {
    BackHandler { onAction(SplitAction.Back) }
    val money = LocalMoneyFormatter.current
    val title = state.total?.let { stringResource(R.string.split_amount_title, money.format(it)) }
        ?: state.groupName?.let { stringResource(R.string.split_in_group_title, it) }
        ?: stringResource(R.string.split_bill)
    Scaffold(topBar = { VoxTopBar(title, onBack = { onAction(SplitAction.Back) }) }) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(VoxSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(VoxSpacing.Medium),
        ) {
            when (state.step) {
                SplitStep.CHOOSE_METHOD -> ChooseMethod(state, onAction)
                SplitStep.EQUAL -> EqualSplit(state, onAction)
                SplitStep.ITEMS -> ItemSplit(state, onAction)
                SplitStep.ITEMS_RESULT -> SplitResult(state, onAction)
            }
        }
    }
}

@Composable
private fun ChooseMethod(state: SplitUiState, onAction: (SplitAction) -> Unit) {
    if (!state.isFromReceipt) {
        OutlinedTextField(
            value = state.description,
            onValueChange = { onAction(SplitAction.DescriptionChanged(it)) },
            label = { Text(stringResource(R.string.field_description)) },
            isError = state.showDescriptionError,
            supportingText = if (state.showDescriptionError) {
                { Text(stringResource(R.string.error_description_required)) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.amountInput,
            onValueChange = { onAction(SplitAction.AmountChanged(it)) },
            label = { Text(stringResource(R.string.field_amount)) },
            prefix = { Text(Currency.getInstance(state.currencyCode).symbol) },
            isError = state.showAmountError,
            supportingText = if (state.showAmountError) {
                { Text(stringResource(R.string.error_amount_invalid)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Text(stringResource(R.string.split_how), style = MaterialTheme.typography.titleMedium)
    MethodCard(VoxIcons.Groups, stringResource(R.string.split_equally), stringResource(R.string.split_equally_body), enabled = true) {
        onAction(SplitAction.ChooseEqual)
    }
    MethodCard(
        VoxIcons.Food,
        stringResource(R.string.split_by_items),
        stringResource(if (state.canSplitByItems) R.string.split_by_items_body else R.string.split_by_items_unavailable),
        enabled = state.canSplitByItems,
    ) { onAction(SplitAction.ChooseItems) }
}

@Composable
private fun MethodCard(icon: ImageVector, title: String, body: String, enabled: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(VoxSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Medium),
        ) {
            IconBadge(icon)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EqualSplit(state: SplitUiState, onAction: (SplitAction) -> Unit) {
    val money = LocalMoneyFormatter.current
    Text(stringResource(R.string.split_who), style = MaterialTheme.typography.titleMedium)
    Column {
        state.people.forEach { person ->
            CheckboxRow(
                label = person.displayName(),
                checked = person.id in state.selectedIds,
                onCheckedChange = { onAction(SplitAction.TogglePerson(person.id)) },
            )
        }
    }
    val count = state.selectedIds.size
    Text(pluralStringResource(R.plurals.people_count, count, count), style = MaterialTheme.typography.bodyLarge)
    state.shares.values.maxOrNull()?.let { share ->
        Text(
            stringResource(R.string.split_each, money.format(Money(share, state.currencyCode))),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
    ConfirmButton(enabled = state.shares.isNotEmpty(), onAction)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemSplit(state: SplitUiState, onAction: (SplitAction) -> Unit) {
    val money = LocalMoneyFormatter.current
    Text(stringResource(R.string.split_who_had_what), style = MaterialTheme.typography.titleMedium)
    state.items.forEach { item ->
        Column(verticalArrangement = Arrangement.spacedBy(VoxSpacing.ExtraSmall)) {
            AmountRow(item.name, money.format(item.amount))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Small)) {
                state.people.forEach { person ->
                    FilterChip(
                        selected = person.id in state.itemAssignees[item.id].orEmpty(),
                        onClick = { onAction(SplitAction.ToggleItemAssignee(item.id, person.id)) },
                        label = { Text(person.displayName()) },
                    )
                }
            }
        }
        HorizontalDivider()
    }
    state.tax?.let {
        AmountRow(stringResource(R.string.receipt_tax), money.format(it))
        Text(stringResource(R.string.split_tax_proportional), style = MaterialTheme.typography.bodySmall)
    }
    if (state.showAssignmentError) {
        Text(stringResource(R.string.error_assign_items), color = MaterialTheme.colorScheme.error)
    }
    Button(
        onClick = { onAction(SplitAction.Calculate) },
        modifier = Modifier.fillMaxWidth().heightIn(min = VoxSize.MinTouchTarget),
    ) { Text(stringResource(R.string.split_calculate)) }
}

@Composable
private fun SplitResult(state: SplitUiState, onAction: (SplitAction) -> Unit) {
    val money = LocalMoneyFormatter.current
    Text(stringResource(R.string.split_result), style = MaterialTheme.typography.titleMedium)
    state.people.filter { it.id in state.shares }.forEach { person ->
        AmountRow(person.displayName(), money.format(Money(state.shares.getValue(person.id), state.currencyCode)))
    }
    ConfirmButton(enabled = true, onAction)
}

@Composable
private fun AmountRow(label: String, amount: String) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(amount, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ConfirmButton(enabled: Boolean, onAction: (SplitAction) -> Unit) {
    Button(
        onClick = { onAction(SplitAction.Confirm) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = VoxSize.MinTouchTarget),
    ) { Text(stringResource(R.string.split_confirm)) }
}

@Preview
@Composable
private fun SplitEqualPreview() {
    val people = listOf(Person(CURRENT_USER_ID, ""), Person("rahul", "Rahul"), Person("riya", "Riya"))
    VoxTheme {
        SplitScreen(
            SplitUiState(
                isLoading = false, step = SplitStep.EQUAL, total = PreviewFixtures.inr(2_035), people = people,
                selectedIds = people.map { it.id }.toSet(), shares = mapOf(CURRENT_USER_ID to 67_834L, "rahul" to 67_833L, "riya" to 67_833L),
            ),
        ) {}
    }
}
