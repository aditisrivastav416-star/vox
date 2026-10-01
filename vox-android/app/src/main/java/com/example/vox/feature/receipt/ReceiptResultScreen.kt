package com.example.vox.feature.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.EmptyState
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
import com.example.vox.domain.model.Money
import com.example.vox.domain.model.Receipt
import com.example.vox.domain.model.ReceiptItem
import java.time.Instant

@Composable
fun ReceiptResultRoute(viewModel: ReceiptResultViewModel, onBack: () -> Unit, onSaved: () -> Unit, onSplit: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { if (it == ReceiptResultEvent.Saved) onSaved() }
    ReceiptResultScreen(state, onBack) { action ->
        if (action == ReceiptResultAction.SplitBill) onSplit(viewModel.receiptId) else viewModel.onAction(action)
    }
}

@Composable
fun ReceiptResultScreen(state: UiState<Receipt?>, onBack: () -> Unit, onAction: (ReceiptResultAction) -> Unit) {
    Scaffold(topBar = { VoxTopBar(stringResource(R.string.receipt_found), onBack = onBack) }) { padding ->
        UiStateContent(state, Modifier.padding(padding)) { receipt ->
            if (receipt == null) {
                EmptyState(stringResource(R.string.receipt_missing_title), stringResource(R.string.receipt_missing_body), Modifier.padding(padding))
                return@UiStateContent
            }
            val money = LocalMoneyFormatter.current
            Column(
                Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(VoxSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(VoxSpacing.Small),
            ) {
                Text(receipt.merchant, style = MaterialTheme.typography.headlineSmall)
                Text(LocalDateFormatter.current.longDate(receipt.timestamp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                receipt.items.forEach { LineItem(it.name, money.format(it.amount)) }
                LineItem(stringResource(R.string.receipt_tax), money.format(receipt.tax))
                HorizontalDivider()
                LineItem(stringResource(R.string.receipt_total), money.format(receipt.total), MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { onAction(ReceiptResultAction.AddTransaction) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = VoxSize.MinTouchTarget).padding(top = VoxSpacing.Medium),
                ) { Text(stringResource(R.string.add_transaction)) }
                OutlinedButton(
                    onClick = { onAction(ReceiptResultAction.SplitBill) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = VoxSize.MinTouchTarget),
                ) { Text(stringResource(R.string.receipt_split)) }
            }
        }
    }
}

@Composable
private fun LineItem(label: String, amount: String, style: TextStyle = MaterialTheme.typography.bodyLarge) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(label, style = style, modifier = Modifier.weight(1f))
        Text(amount, style = style)
    }
}

@Preview
@Composable
private fun ReceiptResultPreview() {
    VoxTheme {
        ReceiptResultScreen(
            UiState.Success(
                Receipt(
                    "r", "Social", Instant.now(),
                    listOf(ReceiptItem("1", "Pasta", PreviewFixtures.inr(650)), ReceiptItem("2", "Pizza", PreviewFixtures.inr(900))),
                    Money(18_500, "INR"),
                ),
            ),
            {}, {},
        )
    }
}
