package com.example.vox.feature.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.DateField
import com.example.vox.core.designsystem.component.LoadingState
import com.example.vox.core.designsystem.component.VoxDatePickerDialog
import com.example.vox.core.designsystem.component.VoxDropdown
import com.example.vox.core.designsystem.component.VoxTopBar
import com.example.vox.core.designsystem.theme.LocalDateFormatter
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents
import com.example.vox.core.ui.labelRes
import com.example.vox.domain.model.PaymentMethod
import com.example.vox.domain.model.TransactionType
import java.util.Currency

@Composable
fun TransactionFormRoute(viewModel: TransactionFormViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { if (it == TransactionFormEvent.Saved) onBack() }
    TransactionFormScreen(state, viewModel::onAction, onBack)
}

@Composable
fun TransactionFormScreen(
    state: TransactionFormUiState,
    onAction: (TransactionFormAction) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            VoxTopBar(
                stringResource(if (state.isEdit) R.string.edit_transaction else R.string.add_transaction),
                onBack = onBack,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(VoxSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(VoxSpacing.Medium),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { i, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { onAction(TransactionFormAction.TypeChanged(type)) },
                        shape = SegmentedButtonDefaults.itemShape(i, TransactionType.entries.size),
                    ) { Text(stringResource(type.labelRes)) }
                }
            }
            OutlinedTextField(
                value = state.amount,
                onValueChange = { onAction(TransactionFormAction.AmountChanged(it)) },
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
            OutlinedTextField(
                value = state.title,
                onValueChange = { onAction(TransactionFormAction.TitleChanged(it)) },
                label = { Text(stringResource(R.string.field_description)) },
                isError = state.showTitleError,
                supportingText = if (state.showTitleError) {
                    { Text(stringResource(R.string.error_description_required)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            VoxDropdown(
                label = stringResource(R.string.field_category),
                selected = state.category,
                options = state.categories,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { onAction(TransactionFormAction.CategoryChanged(it)) },
            )
            DateField(
                label = stringResource(R.string.field_date),
                value = LocalDateFormatter.current.relativeDate(state.date),
                onPick = { onAction(TransactionFormAction.ShowDatePicker(true)) },
            )
            VoxDropdown(
                label = stringResource(R.string.field_payment_method),
                selected = state.paymentMethod,
                options = PaymentMethod.entries,
                optionLabel = { stringResource(it.labelRes) },
                onSelect = { onAction(TransactionFormAction.PaymentMethodChanged(it)) },
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onAction(TransactionFormAction.NotesChanged(it)) },
                label = { Text(stringResource(R.string.field_notes)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onAction(TransactionFormAction.Save) },
                modifier = Modifier.fillMaxWidth().heightIn(min = VoxSize.MinTouchTarget),
            ) { Text(stringResource(if (state.isEdit) R.string.save_changes else R.string.add_transaction)) }
        }
    }
    if (state.showDatePicker) {
        VoxDatePickerDialog(
            initial = state.date,
            onConfirm = { onAction(TransactionFormAction.DateChanged(it)) },
            onDismiss = { onAction(TransactionFormAction.ShowDatePicker(false)) },
        )
    }
}

@Preview
@Composable
private fun TransactionFormPreview() {
    VoxTheme { TransactionFormScreen(TransactionFormUiState(amount = "400", title = "Groceries"), {}, {}) }
}
