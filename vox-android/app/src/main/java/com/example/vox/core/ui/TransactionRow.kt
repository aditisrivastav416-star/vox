package com.example.vox.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.vox.R
import com.example.vox.core.designsystem.component.IconBadge
import com.example.vox.core.designsystem.theme.LocalDateFormatter
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.domain.model.Transaction
import com.example.vox.domain.model.TransactionType

/** Signed amount: expenses are "-₹740", income "+₹20,000" — the sign carries meaning, not just color. */
fun Transaction.signedAmount() = if (type == TransactionType.EXPENSE) -amount else amount

@Composable
fun TransactionRow(
    transaction: Transaction,
    groupName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = LocalMoneyFormatter.current
    val dates = LocalDateFormatter.current
    val subtitle = if (groupName != null) {
        stringResource(R.string.transaction_subtitle, groupName, stringResource(R.string.transaction_shared))
    } else {
        stringResource(
            R.string.transaction_subtitle,
            stringResource(transaction.category?.labelRes ?: R.string.category_uncategorized),
            stringResource(transaction.paymentMethod.labelRes),
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 23.dp, vertical = 4.dp)
            .heightIn(min = VoxSize.MinTouchTarget)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(13.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = VoxSpacing.Medium, vertical = VoxSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Medium),
    ) {
        IconBadge(transaction.category.icon)
        Column(Modifier.weight(1f)) {
            Text(transaction.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                money.formatSigned(transaction.signedAmount()),
                style = MaterialTheme.typography.bodyLarge,
                color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            )
            Text(dates.time(transaction.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionRowPreview() {
    VoxTheme {
        Column {
            TransactionRow(PreviewFixtures.expense, groupName = null, onClick = {})
            TransactionRow(PreviewFixtures.income, groupName = null, onClick = {})
            TransactionRow(PreviewFixtures.expense, groupName = "Goa Trip", onClick = {})
        }
    }
}
