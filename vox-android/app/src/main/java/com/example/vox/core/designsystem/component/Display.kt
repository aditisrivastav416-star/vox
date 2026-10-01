package com.example.vox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.example.vox.R
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.domain.model.Money

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = VoxSize.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        when {
            actionIcon != null -> IconButton(onClick = onAction) { Icon(actionIcon, contentDescription = actionLabel) }
            actionLabel != null -> TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Decorative circular icon; the adjacent text carries the meaning, so no content description. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = VoxSize.CategoryBadge,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = VoxSize.Avatar,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            style = style,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * A balance with an explicit text label, so meaning never relies on color alone.
 * Positive: they owe you. Negative: you owe. Zero: settled.
 */
@Composable
fun BalanceText(
    balance: Money,
    modifier: Modifier = Modifier,
    amountStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val formatter = LocalMoneyFormatter.current
    Column(modifier) {
        when {
            balance.amountMinor > 0 -> {
                Text(formatter.formatSigned(balance), style = amountStyle, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.balance_owed_to_you), style = MaterialTheme.typography.bodySmall)
            }
            balance.amountMinor < 0 -> {
                Text(formatter.formatSigned(balance), style = amountStyle, color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.balance_you_owe), style = MaterialTheme.typography.bodySmall)
            }
            else -> Text(stringResource(R.string.balance_settled), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Label above value, used on detail screens. */
@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = VoxSpacing.Small), verticalArrangement = Arrangement.spacedBy(VoxSpacing.ExtraSmall)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun DisplayPreview() {
    VoxTheme {
        Column(Modifier.padding(VoxSpacing.Medium), verticalArrangement = Arrangement.spacedBy(VoxSpacing.Small)) {
            SectionHeader(title = "Groups", actionLabel = "Add", actionIcon = VoxIcons.Add)
            Row(horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Small)) {
                IconBadge(VoxIcons.Food)
                Avatar("Rahul")
            }
            BalanceText(Money(245_000, "INR"))
            BalanceText(Money(-74_000, "INR"))
            LabeledValue(label = "Category", value = "Food")
        }
    }
}
