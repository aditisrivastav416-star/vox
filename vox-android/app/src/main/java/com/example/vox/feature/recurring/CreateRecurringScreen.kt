package com.example.vox.feature.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.CheckboxRow
import com.example.vox.core.designsystem.component.DateField
import com.example.vox.core.designsystem.component.VoxDatePickerDialog
import com.example.vox.core.designsystem.component.VoxDropdown
import com.example.vox.core.designsystem.component.VoxTopBar
import com.example.vox.core.designsystem.theme.LocalDateFormatter
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents
import com.example.vox.core.ui.labelRes
import com.example.vox.domain.model.Frequency
import java.util.Currency

@Composable
fun CreateRecurringRoute(viewModel: CreateRecurringViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { if (it == CreateRecurringEvent.Saved) onBack() }
    CreateRecurringScreen(state, viewModel::onAction, onBack)
}

@Composable
fun CreateRecurringScreen(state: CreateRecurringUiState, onAction: (CreateRecurringAction) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { VoxTopBar(stringResource(R.string.recurring_new), onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(VoxSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(VoxSpacing.Medium),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(CreateRecurringAction.NameChanged(it)) },
                label = { Text(stringResource(R.string.field_name)) },
                isError = state.showNameError,
                supportingText = if (state.showNameError) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            VoxDropdown(
                label = stringResource(R.string.field_category),
                selected = state.category,
                options = state.categories,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { onAction(CreateRecurringAction.CategoryChanged(it)) },
            )

            Text(stringResource(R.string.field_amount), style = MaterialTheme.typography.titleSmall)
            Column(Modifier.selectableGroup()) {
                AmountModeRow(stringResource(R.string.recurring_fixed_amount), state.isFixedAmount) {
                    onAction(CreateRecurringAction.FixedAmountChanged(true))
                }
                AmountModeRow(stringResource(R.string.recurring_ask_every_time), !state.isFixedAmount) {
                    onAction(CreateRecurringAction.FixedAmountChanged(false))
                }
            }
            if (state.isFixedAmount) {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { onAction(CreateRecurringAction.AmountChanged(it)) },
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

            VoxDropdown(
                label = stringResource(R.string.field_repeats),
                selected = state.frequency,
                options = Frequency.entries,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { onAction(CreateRecurringAction.FrequencyChanged(it)) },
            )
            if (state.frequency == Frequency.CUSTOM) {
                OutlinedTextField(
                    value = state.customIntervalDays,
                    onValueChange = { onAction(CreateRecurringAction.IntervalChanged(it)) },
                    label = { Text(stringResource(R.string.field_interval_days)) },
                    isError = state.showIntervalError,
                    supportingText = if (state.showIntervalError) {
                        { Text(stringResource(R.string.error_interval_invalid)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            DateField(
                label = stringResource(R.string.field_next_due),
                value = LocalDateFormatter.current.dueDate(state.nextDueDate),
                onPick = { onAction(CreateRecurringAction.ShowDatePicker(true)) },
            )

            Text(stringResource(R.string.recurring_remind_me), style = MaterialTheme.typography.titleSmall)
            CheckboxRow(stringResource(R.string.recurring_remind_before), state.remindDaysBefore, {
                onAction(CreateRecurringAction.RemindDaysBeforeChanged(it))
            })
            CheckboxRow(stringResource(R.string.recurring_remind_on_due), state.remindOnDueDate, {
                onAction(CreateRecurringAction.RemindOnDueDateChanged(it))
            })

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { onAction(CreateRecurringAction.Save) },
                modifier = Modifier.fillMaxWidth().heightIn(min = VoxSize.MinTouchTarget),
            ) { Text(stringResource(R.string.save)) }
        }
    }
    if (state.showDatePicker) {
        VoxDatePickerDialog(
            initial = state.nextDueDate,
            onConfirm = { onAction(CreateRecurringAction.DueDateChanged(it)) },
            onDismiss = { onAction(CreateRecurringAction.ShowDatePicker(false)) },
        )
    }
}

@Composable
private fun AmountModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = VoxSize.MinTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = VoxSpacing.Small))
    }
}

@Preview
@Composable
private fun CreateRecurringPreview() {
    VoxTheme { CreateRecurringScreen(CreateRecurringUiState(name = "Rent", amount = "18000"), {}, {}) }
}
