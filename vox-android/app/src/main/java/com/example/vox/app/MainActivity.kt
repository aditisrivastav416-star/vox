package com.example.vox.app

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.designsystem.theme.VoxAppearance
import com.example.vox.navigation.VoxApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        VoxAppearance.restore(this)
        setContent {
            VoxTheme(darkTheme = VoxAppearance.dark) { VoxApp() }
        }
    }
}
