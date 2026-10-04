package com.example.vox.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vox.R
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.labelRes
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.Money
import com.example.vox.feature.home.CategoryAmountUi
import com.example.vox.feature.home.SpendingSummaryUi
import com.example.vox.feature.home.SummaryMode
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SpendingSummaryCard(
    summary: SpendingSummaryUi,
    month: YearMonth,
    showAllCategories: Boolean,
    onSelectMode: (SummaryMode) -> Unit,
    onToggleBreakdown: () -> Unit,
    modifier: Modifier = Modifier,
    budgetMinor: Long? = null,
    onSetBudget: () -> Unit = {},
) {
    val money = LocalMoneyFormatter.current
    val budget = budgetMinor?.takeIf { it > 0 }
    val spent = summary.spending.amountMinor.coerceAtLeast(0)
    val used = budget?.let { spent.toDouble() / it } ?: 0.0
    val monthLabel = month.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(
            Brush.linearGradient(listOf(Color(0xFF24264E), Color(0xFF36316C), Color(0xFF7061CD)))
        )) {
            Canvas(Modifier.matchParentSize()) {
                val barWidth = 26.dp.toPx()
                val barHeight = 106.dp.toPx()
                repeat(3) { index ->
                    val x = size.width * .82f + index * 28.dp.toPx()
                    val y = 34.dp.toPx()
                    rotate(-28f, pivot = Offset(x + barWidth / 2, y + barHeight / 2)) {
                        drawRoundRect(Color.White.copy(alpha = .11f), Offset(x, y), Size(barWidth, barHeight),
                            CornerRadius(14.dp.toPx()), style = Stroke(3.dp.toPx()))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL SPENT THIS MONTH", color = Color(0xFFD9D8F4), fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.background(Color.White.copy(alpha = .14f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(VoxIcons.Calendar, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(" $monthLabel", color = Color.White, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(money.format(summary.spending), color = Color.White, fontSize = 39.sp, letterSpacing = (-1.5).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .16f)))
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Monthly budget", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onSetBudget, modifier = Modifier.size(32.dp)) {
                    Icon(VoxIcons.Edit, contentDescription = "Edit monthly budget", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(if (budget == null) "Set budget" else "${(used * 100).roundToInt()}% used", color = Color(0xFFC9B9FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = .24f), RoundedCornerShape(50))) {
                if (budget != null && used > 0) Box(Modifier.fillMaxWidth(used.toFloat().coerceIn(0f, 1f)).height(8.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF82D4FA), Color(0xFFBBA5FF))), RoundedCornerShape(50)))
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (budget == null) "Set a budget to track what's left" else
                    "${money.format(Money((budget - spent).coerceAtLeast(0), summary.spending.currencyCode))} left to spend",
                    color = Color(0xFFD9D8F4), fontSize = 11.sp)
                if (budget != null) Text("of ${money.format(Money(budget, summary.spending.currencyCode))}", color = Color(0xFFD9D8F4), fontSize = 11.sp)
            }
            }
        }
        Column {
            Text("THE BREAKDOWN", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Where it went", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp)
                TextButton(onClick = onToggleBreakdown) { Text(if (showAllCategories) "Show less" else "View all →", fontSize = 12.sp) }
            }
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(138.dp), contentAlignment = Alignment.Center) {
                    val colors = listOf(Color(0xFF795DE8), Color(0xFF9D8AF7), Color(0xFF56A4E9), Color(0xFF8DD0F5), Color(0xFFBAC5DE))
                    Canvas(Modifier.size(134.dp)) {
                        val ring = 22.dp.toPx()
                        val inset = ring / 2
                        val arcTopLeft = Offset(inset, inset)
                        val arcSize = Size(size.minDimension - ring, size.minDimension - ring)
                        drawArc(Color(0xFFEAEAF2), 0f, 360f, false, topLeft = arcTopLeft, size = arcSize, style = Stroke(ring))
                        var start = -90f
                        summary.categories.forEachIndexed { index, item ->
                            val sweep = item.fraction.coerceIn(0f, 1f) * 360f
                            drawArc(colors[index % colors.size], start, sweep, false, topLeft = arcTopLeft, size = arcSize, style = Stroke(ring))
                            start += sweep
                        }
                        drawArc(Color(0xFF202345).copy(alpha = .15f), 0f, 360f, false,
                            topLeft = Offset(ring, ring),
                            size = Size(size.minDimension - ring * 2, size.minDimension - ring * 2),
                            style = Stroke(3.dp.toPx()))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SPENDING", fontSize = 9.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(money.format(summary.spending), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text("this month", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    if (summary.categories.isEmpty()) Text(stringResource(R.string.summary_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    summary.categories.forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(listOf(Color(0xFF795DE8), Color(0xFF9D8AF7), Color(0xFF56A4E9), Color(0xFF8DD0F5), Color(0xFFBAC5DE))[index % 5], RoundedCornerShape(50)))
                            Text(stringResource(item.category.labelRes), Modifier.weight(1f).padding(start = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                            Text("${(item.fraction * 100).roundToInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        YearMonth.now(), false, {}, {}, Modifier.padding(20.dp), budgetMinor = 420000,
    ) }
}
