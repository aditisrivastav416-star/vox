package com.example.vox.feature.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.vox.R
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxRadius
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.feature.home.ChartBarUi
import kotlin.math.abs
import kotlin.math.max

/**
 * Weekly column chart. Negative values (NET mode) are drawn below the baseline
 * in the error color; TalkBack reads every week's value instead of the drawing.
 */
@Composable
fun SpendingChart(bars: List<ChartBarUi>, modifier: Modifier = Modifier) {
    val money = LocalMoneyFormatter.current
    val positive = MaterialTheme.colorScheme.primary
    val negative = MaterialTheme.colorScheme.error
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val weekLabels = bars.map { stringResource(R.string.chart_week_short, it.weekNumber) }
    val description = bars.map { stringResource(R.string.chart_week_value, it.weekNumber, money.formatSigned(it.value)) }
        .joinToString()

    Column(modifier.clearAndSetSemantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(VoxSize.ChartHeight)) {
            if (bars.isEmpty()) return@Canvas
            val maxUp = bars.maxOf { max(it.value.amountMinor, 0) }.toFloat()
            val maxDown = bars.maxOf { max(-it.value.amountMinor, 0) }.toFloat()
            val range = (maxUp + maxDown).takeIf { it > 0 } ?: 1f
            val baseline = size.height * maxUp / range
            val slot = size.width / bars.size
            val barWidth = slot * 0.5f
            val radius = CornerRadius(VoxRadius.Small.toPx())
            bars.forEachIndexed { i, bar ->
                val height = size.height * abs(bar.value.amountMinor) / range
                val left = slot * i + (slot - barWidth) / 2
                val top = if (bar.value.amountMinor >= 0) baseline - height else baseline
                drawRoundRect(
                    color = if (bar.value.amountMinor >= 0) positive else negative,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height),
                    cornerRadius = radius,
                )
            }
            drawLine(baselineColor, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = VoxSize.Divider.toPx())
        }
        Row(Modifier.fillMaxWidth().padding(top = VoxSpacing.ExtraSmall), horizontalArrangement = Arrangement.SpaceAround) {
            weekLabels.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpendingChartPreview() {
    VoxTheme {
        SpendingChart(
            bars = listOf(4_200L, 1_800L, -900L, 6_100L, 300L).mapIndexed { i, v -> ChartBarUi(i + 1, PreviewFixtures.inr(v)) },
            modifier = Modifier.padding(VoxSpacing.Medium),
        )
    }
}
