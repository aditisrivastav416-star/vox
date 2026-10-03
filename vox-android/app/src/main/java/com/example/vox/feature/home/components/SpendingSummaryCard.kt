package com.example.vox.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vox.R
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.labelRes
import com.example.vox.domain.model.Category
import com.example.vox.feature.home.CategoryAmountUi
import com.example.vox.feature.home.SpendingSummaryUi
import com.example.vox.feature.home.SummaryMode

@Composable
fun SpendingSummaryCard(
    summary: SpendingSummaryUi,
    showAllCategories: Boolean,
    onSelectMode: (SummaryMode) -> Unit,
    onToggleBreakdown: () -> Unit,
    modifier: Modifier = Modifier,
    budgetMinor: Long? = null,
    onSetBudget: () -> Unit = {},
) {
    val money = LocalMoneyFormatter.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF1D204B), Color(0xFF39316F), Color(0xFF7361D0))), RoundedCornerShape(17.dp)
            ).padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text("TOTAL SPENT THIS MONTH", color = Color(0xFFD9D8F4), fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            Text(money.format(summary.spending), color = Color.White, fontSize = 35.sp, letterSpacing = (-1.5).sp, fontWeight = FontWeight.Bold)
            if (budgetMinor != null && budgetMinor > 0) {
                val used = summary.spending.amountMinor.toFloat() / budgetMinor
                LinearProgressIndicator(progress = { used.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Color.White,
                    trackColor = Color.White.copy(alpha = .2f))
                Text("${(used * 100).toInt()}% of monthly budget used", color = Color.White, fontSize = 12.sp)
            }
            TextButton(onClick = onSetBudget) {
                Text(if (budgetMinor == null) "Set monthly budget" else "Edit monthly budget", color = Color.White)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .16f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monthly spending", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(summary.changePercent?.let { "%+d%% vs last month".format(it) } ?: "This month", color = Color(0xFFD9D8F4), fontSize = 11.sp)
            }
        }
        Column {
            Text("THE BREAKDOWN", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Where it went", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp)
                TextButton(onClick = onToggleBreakdown) { Text(if (showAllCategories) "Show less" else "View all →", fontSize = 12.sp) }
            }
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(17.dp)).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(135.dp), contentAlignment = Alignment.Center) {
                    val colors = listOf(Color(0xFF795DE8), Color(0xFF9D8AF7), Color(0xFF56A4E9), Color(0xFF8DD0F5), Color(0xFFBAC5DE))
                    Canvas(Modifier.size(125.dp)) {
                        drawArc(Color(0xFFEAEAF2), 0f, 360f, false, style = Stroke(17.dp.toPx()))
                        var start = -90f
                        summary.categories.forEachIndexed { index, item ->
                            val sweep = item.fraction.coerceIn(0f, 1f) * 360f
                            drawArc(colors[index % colors.size], start, sweep, false, style = Stroke(17.dp.toPx()))
                            start += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SPENDING", fontSize = 8.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(money.format(summary.spending), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    if (summary.categories.isEmpty()) Text(stringResource(R.string.summary_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    summary.categories.forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(listOf(Color(0xFF795DE8), Color(0xFF9D8AF7), Color(0xFF56A4E9), Color(0xFF8DD0F5), Color(0xFFBAC5DE))[index % 5], RoundedCornerShape(50)))
                            Text(stringResource(item.category.labelRes), Modifier.weight(1f).padding(start = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                            Text("${(item.fraction * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSummary() {
    VoxTheme { SpendingSummaryCard(
        SpendingSummaryUi(SummaryMode.SPENDING, PreviewFixtures.inr(2840), PreviewFixtures.inr(4200), PreviewFixtures.inr(2840), -12,
            emptyList(), listOf(CategoryAmountUi(Category.FOOD, PreviewFixtures.inr(400), .15f))),
        false, {}, {}, Modifier.padding(20.dp),
    ) }
}
