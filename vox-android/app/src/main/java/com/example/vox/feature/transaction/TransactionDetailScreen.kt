package com.example.vox.feature.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.EmptyState
import com.example.vox.core.designsystem.component.LabeledValue
import com.example.vox.core.designsystem.component.UiStateContent
import com.example.vox.core.designsystem.component.VoxTopBar
import com.example.vox.core.designsystem.theme.LocalDateFormatter
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.labelRes
import com.example.vox.core.ui.signedAmount

@Composable
fun TransactionDetailRoute(viewModel: TransactionDetailViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { if (it == TransactionDetailEvent.Deleted) onBack() }
    TransactionDetailScreen(state, onBack) { action ->
        if (action == TransactionDetailAction.Edit) onEdit() else viewModel.onAction(action)
    }
}

@Composable
fun TransactionDetailScreen(
    state: UiState<TransactionDetailUiModel>,
    onBack: () -> Unit,
    onAction: (TransactionDetailAction) -> Unit,
) {
    val title = ((state as? UiState.Success)?.data?.transaction?.type?.labelRes)?.let { stringResource(it) }.orEmpty()
    Scaffold(topBar = { VoxTopBar(title, onBack = onBack) }) { padding ->
        UiStateContent(state, Modifier.padding(padding)) { model ->
            val tx = model.transaction
            if (tx == null) {
                EmptyState(
                    title = stringResource(R.string.transaction_not_found),
                    body = stringResource(R.string.transaction_not_found_body),
                    modifier = Modifier.padding(padding),
                )
                return@UiStateContent
            }
            val dates = LocalDateFormatter.current
            Column(
                Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(VoxSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(VoxSpacing.ExtraSmall),
            ) {
                Column(Modifier.fillMaxWidth().padding(vertical = VoxSpacing.Large), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(LocalMoneyFormatter.current.formatSigned(tx.signedAmount()), style = MaterialTheme.typography.displaySmall)
                    Text(tx.title, style = MaterialTheme.typography.titleLarge)
                    Text(dates.longDate(tx.timestamp), style = MaterialTheme.typography.bodyLarge)
                    Text(dates.time(tx.timestamp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LabeledValue(stringResource(R.string.field_category), stringResource(tx.category?.labelRes ?: R.string.category_uncategorized))
                LabeledValue(stringResource(R.string.field_payment_method), stringResource(tx.paymentMethod.labelRes))
                LabeledValue(stringResource(R.string.field_source), stringResource(tx.source.labelRes))
                LabeledValue(stringResource(R.string.field_group), model.groupName ?: stringResource(R.string.value_none))
                LabeledValue(stringResource(R.string.field_notes), tx.notes ?: stringResource(R.string.value_empty))
                Button(
                    onClick = { onAction(TransactionDetailAction.Edit) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = VoxSize.MinTouchTarget).padding(top = VoxSpacing.Medium),
                ) { Text(stringResource(R.string.edit_transaction)) }
                TextButton(
                    onClick = { onAction(TransactionDetailAction.ShowDeleteConfirmation(true)) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.delete_transaction)) }
            }
            if (model.showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { onAction(TransactionDetailAction.ShowDeleteConfirmation(false)) },
                    title = { Text(stringResource(R.string.delete_transaction_title)) },
                    text = { Text(stringResource(R.string.delete_transaction_body)) },
                    confirmButton = {
                        TextButton(onClick = { onAction(TransactionDetailAction.ConfirmDelete) }) { Text(stringResource(R.string.delete)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { onAction(TransactionDetailAction.ShowDeleteConfirmation(false)) }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun TransactionDetailPreview() {
    VoxTheme {
        TransactionDetailScreen(UiState.Success(TransactionDetailUiModel(PreviewFixtures.expense, null, false)), {}, {})
    }
}
