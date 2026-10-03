package com.example.vox.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.BalanceText
import com.example.vox.core.designsystem.component.EmptyState
import com.example.vox.core.designsystem.component.IconBadge
import com.example.vox.core.designsystem.component.UiStateContent
import com.example.vox.core.designsystem.component.VoxTopBar
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.UiState
import com.example.vox.core.ui.displayName
import com.example.vox.core.ui.icon
import com.example.vox.domain.model.Money

@Composable
fun GroupsRoute(viewModel: GroupsViewModel, onBack: () -> Unit, onCreate: () -> Unit, onOpenGroup: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupsScreen(state, onBack, onCreate, onOpenGroup)
}

@Composable
fun GroupsScreen(state: UiState<GroupsUiModel>, onBack: () -> Unit, onCreate: () -> Unit, onOpenGroup: (String) -> Unit) {
    Scaffold(
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onCreate, icon = { Icon(VoxIcons.Add, contentDescription = null) }, text = { Text(stringResource(R.string.create_group)) }) },
        topBar = {
            VoxTopBar(stringResource(R.string.groups), onBack = onBack) {
                IconButton(onClick = onCreate) { Icon(VoxIcons.Add, contentDescription = stringResource(R.string.create_group)) }
            }
        },
    ) { padding ->
        UiStateContent(state, Modifier.padding(padding)) { model ->
            if (model.groups.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.groups_empty_title),
                    body = stringResource(R.string.groups_empty_body),
                    actionLabel = stringResource(R.string.groups_empty_action),
                    onAction = onCreate,
                    modifier = Modifier.padding(padding),
                )
                return@UiStateContent
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = VoxSpacing.Medium, end = VoxSpacing.Medium,
                    top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + VoxSpacing.Medium,
                ),
                verticalArrangement = Arrangement.spacedBy(VoxSpacing.Small),
            ) {
                item(key = "totals") { Totals(model.owedToYou, model.youOwe) }
                items(model.groups, key = { it.group.id }) { item ->
                    GroupCard(item, onClick = { onOpenGroup(item.group.id) })
                }
            }
        }
    }
}

@Composable
private fun Totals(owedToYou: Money, youOwe: Money) {
    val money = LocalMoneyFormatter.current
    Row(Modifier.fillMaxWidth().padding(vertical = VoxSpacing.Small), horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Medium)) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Text(stringResource(R.string.groups_owed_to_you), style = MaterialTheme.typography.labelLarge)
            Text(money.format(owedToYou), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Text(stringResource(R.string.groups_you_owe), style = MaterialTheme.typography.labelLarge)
            Text(money.format(youOwe), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun GroupCard(item: GroupListItemUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val money = LocalMoneyFormatter.current
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(VoxSpacing.Medium), verticalArrangement = Arrangement.spacedBy(VoxSpacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Medium)) {
                IconBadge(item.group.type.icon)
                Text(item.group.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                BalanceText(item.balance)
            }
            item.memberBalances.forEach { member ->
                Row(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(member.person.displayName(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(money.formatSigned(member.balance), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview
@Composable
private fun GroupsScreenPreview() {
    val group = PreviewFixtures.group
    VoxTheme {
        GroupsScreen(
            UiState.Success(
                GroupsUiModel(
                    PreviewFixtures.inr(2_400), PreviewFixtures.inr(0),
                    listOf(GroupListItemUi(group, group.netBalance, group.memberBalances())),
                ),
            ),
            {}, {}, {},
        )
    }
}

@Preview
@Composable
private fun GroupsScreenEmptyPreview() {
    VoxTheme {
        GroupsScreen(UiState.Success(GroupsUiModel(PreviewFixtures.inr(0), PreviewFixtures.inr(0), emptyList())), {}, {}, {})
    }
}
