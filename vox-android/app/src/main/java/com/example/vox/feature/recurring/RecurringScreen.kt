package com.example.vox.feature.recurring

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.EmptyState
import com.example.vox.core.designsystem.component.SectionHeader
import com.example.vox.core.designsystem.component.UiStateContent
import com.example.vox.core.designsystem.component.VoxTopBar
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.RecurringPaymentRow
import com.example.vox.core.ui.UiState
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Frequency
import com.example.vox.domain.model.RecurringPayment
import java.time.LocalDate

@Composable
fun RecurringRoute(viewModel: RecurringViewModel, onBack: () -> Unit, onCreate: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecurringScreen(state, onBack, onCreate)
}

@Composable
fun RecurringScreen(state: UiState<List<RecurringPayment>>, onBack: () -> Unit, onCreate: () -> Unit) {
    Scaffold(
        topBar = {
            VoxTopBar(stringResource(R.string.recurring_title), onBack = onBack) {
                IconButton(onClick = onCreate) { Icon(VoxIcons.Add, contentDescription = stringResource(R.string.recurring_new)) }
            }
        },
    ) { padding ->
        UiStateContent(state, Modifier.padding(padding)) { payments ->
            if (payments.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.recurring_empty_title),
                    body = stringResource(R.string.recurring_empty_body),
                    actionLabel = stringResource(R.string.recurring_new),
                    onAction = onCreate,
                    modifier = Modifier.padding(padding),
                )
                return@UiStateContent
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                item(key = "header") {
                    SectionHeader(stringResource(R.string.upcoming_payments), Modifier.padding(horizontal = VoxSpacing.Medium))
                }
                items(payments, key = { it.id }) { RecurringPaymentRow(it) }
            }
        }
    }
}

@Preview
@Composable
private fun RecurringScreenPreview() {
    VoxTheme {
        RecurringScreen(
            UiState.Success(
                listOf(
                    RecurringPayment("1", "Rent", Category.RENT, PreviewFixtures.inr(18_000), Frequency.MONTHLY, nextDueDate = LocalDate.now().plusDays(4)),
                    RecurringPayment("2", "Netflix", Category.ENTERTAINMENT, PreviewFixtures.inr(649), Frequency.MONTHLY, nextDueDate = LocalDate.now().plusDays(13)),
                ),
            ),
            {}, {},
        )
    }
}

@Preview
@Composable
private fun RecurringScreenEmptyPreview() {
    VoxTheme { RecurringScreen(UiState.Success(emptyList()), {}, {}) }
}
