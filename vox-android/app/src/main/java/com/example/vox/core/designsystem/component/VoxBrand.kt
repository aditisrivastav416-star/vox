package com.example.vox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vox.core.designsystem.theme.VoxDisplayFont

@Composable
fun VoxBrand(modifier: Modifier = Modifier, light: Boolean = false, iconSize: Dp = 31.dp, dotColor: Color = Color(0xFF8D72F7)) {
    val wordColor = if (light) Color.White else MaterialTheme.colorScheme.onBackground
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (iconSize >= 70.dp) 19.dp else 8.dp)) {
        Box(
            Modifier.size(iconSize).rotate(-9f).background(
                Brush.linearGradient(listOf(Color(0xFFA78AFF), Color(0xFF6145E7), Color(0xFF3C65CF))),
                RoundedCornerShape(iconSize / 3),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(.26f, .55f, .40f, .26f).forEach { fraction ->
                    Box(Modifier.width(iconSize * .10f).height(iconSize * fraction).background(Color.White, RoundedCornerShape(3.dp)))
                }
            }
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = wordColor)) { append("vox") }
                withStyle(SpanStyle(color = dotColor)) { append(".") }
            },
            fontSize = (iconSize.value * 1.03f).sp,
            letterSpacing = (-2).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = VoxDisplayFont,
            lineHeight = (iconSize.value * 1.04f).sp,
        )
    }
}
