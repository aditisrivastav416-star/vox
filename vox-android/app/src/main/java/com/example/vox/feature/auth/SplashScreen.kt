package com.example.vox.feature.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.vox.core.designsystem.component.VoxBrand
import com.example.vox.core.designsystem.theme.VoxAppearance
import com.example.vox.core.designsystem.theme.VoxBodyFont

@Composable
fun SplashScreen() {
    val activity = LocalContext.current as? Activity
    val view = LocalView.current
    DisposableEffect(activity, view) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = !VoxAppearance.dark
            controller?.isAppearanceLightNavigationBars = !VoxAppearance.dark
        }
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF454098), Color(0xFF343078), Color(0xFF24215C), Color(0xFF171841)))
        ),
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 80.dp, y = (-100).dp).size(330.dp).background(
            Brush.radialGradient(listOf(Color(0xAA9E94FF), Color(0x447A70DC), Color.Transparent)), CircleShape
        ))
        Box(Modifier.align(Alignment.BottomStart).offset(x = (-110).dp, y = 110.dp).size(320.dp).background(
            Brush.radialGradient(listOf(Color(0x99567CE7), Color.Transparent)), CircleShape
        ))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            VoxBrand(light = true, iconSize = 77.dp, dotColor = Color(0xFFB8A9FF))
            Text("Money moves better with you.", fontFamily = VoxBodyFont, color = Color(0xFFCBC9EC), fontSize = 14.sp)
        }
        Text("YOUR MONEY, IN MOTION", Modifier.align(Alignment.BottomCenter).offset(y = (-45).dp),
            fontFamily = VoxBodyFont, color = Color(0xFFB9B6E0), fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 2.8.sp)
    }
}
