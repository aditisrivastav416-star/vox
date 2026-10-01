package com.example.vox.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.vox.core.designsystem.component.BalanceText
import com.example.vox.core.designsystem.component.IconBadge
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.icon
import com.example.vox.domain.model.GroupType
import com.example.vox.feature.home.GroupUi

@Composable
fun GroupSummaryCard(group: GroupUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.width(VoxSize.GroupCardWidth)) {
        Column(Modifier.padding(VoxSpacing.Medium), verticalArrangement = Arrangement.spacedBy(VoxSpacing.Small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Small)) {
                IconBadge(group.type.icon)
                Text(group.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BalanceText(group.balance)
        }
    }
}

@Preview
@Composable
private fun GroupSummaryCardPreview() {
    VoxTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Small)) {
            GroupSummaryCard(GroupUi("1", "Goa Trip", GroupType.TRIP, PreviewFixtures.inr(2_450)), onClick = {})
            GroupSummaryCard(GroupUi("2", "Flat 402", GroupType.HOME, PreviewFixtures.inr(-740)), onClick = {})
        }
    }
}
