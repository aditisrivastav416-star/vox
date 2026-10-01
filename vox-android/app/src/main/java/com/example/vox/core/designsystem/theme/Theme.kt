package com.example.vox.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.vox.core.common.AndroidVoxDateFormatter
import com.example.vox.core.common.MoneyFormatter
import com.example.vox.core.common.VoxDateFormatter

val LocalMoneyFormatter = staticCompositionLocalOf { MoneyFormatter() }
val LocalDateFormatter = staticCompositionLocalOf<VoxDateFormatter> { error("VoxDateFormatter not provided") }

object VoxAppearance {
    var dark by mutableStateOf(false)
        private set

    fun restore(context: Context) {
        dark = context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getBoolean("dark", false)
    }

    fun toggle(context: Context) {
        dark = !dark
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putBoolean("dark", dark).apply()
    }
}

@Composable
fun VoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { AndroidVoxDateFormatter(context) }
    CompositionLocalProvider(LocalDateFormatter provides dateFormatter) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = VoxTypography,
            shapes = VoxShapes,
            content = content,
        )
    }
}
