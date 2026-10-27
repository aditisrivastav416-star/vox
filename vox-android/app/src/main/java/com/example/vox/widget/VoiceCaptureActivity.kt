package com.example.vox.widget

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.withResumed
import com.example.vox.R
import com.example.vox.app.MainActivity
import com.example.vox.app.VoxApplication
import com.example.vox.core.designsystem.theme.VoxAppearance
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents
import com.example.vox.feature.vox.VoxAction
import com.example.vox.feature.vox.VoxEvent
import com.example.vox.feature.vox.VoxScreen
import com.example.vox.feature.vox.VoxUiState
import com.example.vox.feature.vox.VoxViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A separate task keeps the launcher underneath the recorder, even when Vox is already open. */
class VoiceCaptureActivity : ComponentActivity() {
    private val container get() = (application as VoxApplication).container
    private val viewModel by viewModels<VoxViewModel> {
        viewModelFactory {
            initializer {
                VoxViewModel(container.voiceAssistant, container.transactionRepository, container.budgetRepository, transactionsOnly = true)
            }
        }
    }
    private var permissionDenied by mutableStateOf(false)
    private var startRecording: Job? = null
    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startRecording = lifecycleScope.launch {
                lifecycle.withResumed { viewModel.onAction(VoxAction.StartListening) }
            }
        } else {
            permissionDenied = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        VoxAppearance.restore(this)
        setContent {
            VoxTheme(darkTheme = VoxAppearance.dark) {
                if (!container.userRepository.hasProfile()) {
                    AlertDialog(
                        onDismissRequest = ::finishAndRemoveTask,
                        title = { Text(stringResource(R.string.widget_setup_title)) },
                        text = { Text(stringResource(R.string.widget_setup_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                startActivity(Intent(this, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                                finishAndRemoveTask()
                            }) { Text(stringResource(R.string.widget_open_app)) }
                        },
                        dismissButton = { TextButton(onClick = ::finishAndRemoveTask) { Text(stringResource(R.string.cancel)) } },
                    )
                } else {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    val language by viewModel.languageCode.collectAsStateWithLifecycle()
                    var requested by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        lifecycle.withResumed {
                            if (!requested) {
                                requested = true
                                microphone.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                    BackHandler(enabled = (state as? VoxUiState.Recognized)?.isSaving == true) { }
                    ObserveEvents(viewModel.events) {
                        if (it == VoxEvent.Saved) {
                            Toast.makeText(this, R.string.widget_saved, Toast.LENGTH_SHORT).show()
                            finishAndRemoveTask()
                        }
                    }
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                        VoxScreen(state, language, quickCapture = true) { action ->
                            when (action) {
                                VoxAction.Close -> finishAndRemoveTask()
                                VoxAction.StartListening -> microphone.launch(Manifest.permission.RECORD_AUDIO)
                                else -> viewModel.onAction(action)
                            }
                        }
                    }
                    if (permissionDenied) {
                        AlertDialog(
                            onDismissRequest = { permissionDenied = false },
                            title = { Text(stringResource(R.string.widget_permission_title)) },
                            text = { Text(stringResource(R.string.widget_permission_message)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    permissionDenied = false
                                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                                }) { Text(stringResource(R.string.widget_open_settings)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { permissionDenied = false }) { Text(stringResource(R.string.cancel)) }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        startRecording?.cancel()
        if (viewModel.state.value == VoxUiState.Listening) viewModel.onAction(VoxAction.CancelListening)
        super.onStop()
    }
}
