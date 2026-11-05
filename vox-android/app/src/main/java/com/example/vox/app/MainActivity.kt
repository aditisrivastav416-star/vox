package com.example.vox.app

import android.os.Bundle
import android.os.Build
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.vox.R
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.designsystem.theme.VoxAppearance
import com.example.vox.navigation.VoxApp
import com.example.vox.widget.VoiceTransactionWidget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        VoxAppearance.restore(this)
        val preferences = getSharedPreferences("onboarding", MODE_PRIVATE)
        setContent {
            var showWidgetPrompt by androidx.compose.runtime.remember { mutableStateOf(!preferences.getBoolean("widget_prompt_shown", false)) }
            VoxTheme(darkTheme = VoxAppearance.dark) {
                VoxApp()
                if (showWidgetPrompt) AlertDialog(
                    onDismissRequest = { preferences.edit().putBoolean("widget_prompt_shown", true).apply(); showWidgetPrompt = false },
                    title = { Text(stringResource(R.string.widget_add)) },
                    text = { Text(stringResource(R.string.widget_add_manually)) },
                    confirmButton = { TextButton(onClick = {
                        preferences.edit().putBoolean("widget_prompt_shown", true).apply()
                        showWidgetPrompt = false
                        val manager = getSystemService(AppWidgetManager::class.java)
                        if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(
                            ComponentName(this, VoiceTransactionWidget::class.java), null, null)
                    }) { Text(stringResource(R.string.widget_add)) } },
                    dismissButton = { TextButton(onClick = {
                        preferences.edit().putBoolean("widget_prompt_shown", true).apply(); showWidgetPrompt = false
                    }) { Text("Later") } },
                )
            }
        }
    }
}
