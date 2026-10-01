package com.example.vox.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.vox.R
import com.example.vox.core.designsystem.component.IconBadge
import com.example.vox.core.designsystem.theme.LocalDateFormatter
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Frequency
import com.example.vox.domain.model.RecurringPayment
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun RecurringPaymentRow(
    payment: RecurringPayment,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val money = LocalMoneyFormatter.current
    val dates = LocalDateFormatter.current
    val daysLeft = ChronoUnit.DAYS.between(today, payment.nextDueDate).toInt()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .heightIn(min = VoxSize.MinTouchTarget)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = VoxSpacing.Medium, vertical = VoxSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VoxSpacing.Medium),
    ) {
        IconBadge(payment.category.icon)
        Column(Modifier.weight(1f)) {
            Text(payment.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.recurring_schedule, stringResource(payment.frequency.labelRes), dates.dueDate(payment.nextDueDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(payment.amount?.let(money::format) ?: stringResource(R.string.recurring_amount_needed),
                style = MaterialTheme.typography.bodyMedium)
            Text(
                when {
                    daysLeft < 0 -> stringResource(R.string.recurring_overdue)
                    daysLeft == 0 -> stringResource(R.string.recurring_due_today)
                    else -> pluralStringResource(R.plurals.due_in_days, daysLeft, daysLeft)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (daysLeft <= 3) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecurringPaymentRowPreview() {
    VoxTheme {
        Column {
            RecurringPaymentRow(RecurringPayment("1", "Rent", Category.RENT, PreviewFixtures.inr(18_000), Frequency.MONTHLY, nextDueDate = LocalDate.now().plusDays(4)))
            RecurringPaymentRow(RecurringPayment("2", "Electricity", Category.BILLS, null, Frequency.MONTHLY, nextDueDate = LocalDate.now().plusDays(7)))
        }
    }
}
