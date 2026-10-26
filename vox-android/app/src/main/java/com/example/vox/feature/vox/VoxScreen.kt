package com.example.vox.feature.vox

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.core.designsystem.component.LabeledValue
import com.example.vox.core.designsystem.component.VoxBrand
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.LocalMoneyFormatter
import com.example.vox.core.designsystem.theme.VoxSize
import com.example.vox.core.designsystem.theme.VoxSpacing
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents
import com.example.vox.core.ui.PreviewFixtures
import com.example.vox.core.ui.labelRes
import com.example.vox.domain.model.Category
import com.example.vox.domain.model.TransactionType
import com.example.vox.domain.model.VoiceResult

private const val PULSE_MS = 700
private const val PULSE_SCALE = 1.15f

@Composable
fun VoxRoute(
    viewModel: VoxViewModel,
    onClose: () -> Unit,
    onTypeInstead: (String) -> Unit,
    onScanReceipt: () -> Unit,
    onSplitBill: () -> Unit,
    onEditDetails: (VoiceResult) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val languageCode by viewModel.languageCode.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) viewModel.onAction(VoxAction.StartListening)
    }
    ObserveEvents(viewModel.events) { if (it == VoxEvent.Saved) onClose() }
    VoxScreen(state, languageCode) { action ->
        when (action) {
            VoxAction.Close -> onClose()
            is VoxAction.TypeInstead -> onTypeInstead(action.text)
            VoxAction.ScanReceipt -> onScanReceipt()
            VoxAction.SplitBill -> onSplitBill()
            is VoxAction.EditDetails -> onEditDetails(action.result)
            VoxAction.StartListening -> permission.launch(Manifest.permission.RECORD_AUDIO)
            else -> viewModel.onAction(action)
        }
    }
}

@Composable
fun VoxScreen(state: VoxUiState, languageCode: String, onAction: (VoxAction) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.62f).background(MaterialTheme.colorScheme.background,
            RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).safeDrawingPadding().padding(horizontal = 24.dp)) {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 38.dp, height = 4.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
            }
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                VoxBrand(Modifier.weight(1f))
                IconButton(onClick = { onAction(VoxAction.Close) }) {
                    Icon(VoxIcons.Close, contentDescription = stringResource(R.string.cd_close))
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (state) {
                    VoxUiState.Idle -> IdleContent(languageCode, onAction)
                    VoxUiState.Listening -> ListeningContent(onAction)
                    is VoxUiState.Recognized -> RecognizedContent(state.result, onAction)
                    is VoxUiState.Answer -> {
                        Text(state.transcript, style = MaterialTheme.typography.bodyMedium)
                        Text(state.reply, style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        Button(onClick = { onAction(VoxAction.StartListening) }) { Text("Ask another question") }
                    }
                    is VoxUiState.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { onAction(VoxAction.StartListening) }) { Text("Try recording again") }
                        androidx.compose.material3.TextButton(onClick = { onAction(VoxAction.TypeInstead()) }) { Text("Type instead") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleContent(languageCode: String, onAction: (VoxAction) -> Unit) {
    var text by remember { mutableStateOf("") }
    Box(Modifier.size(150.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
        MicButton(scale = 1f, onClick = { onAction(VoxAction.StartListening) })
    }
    Text("YOUR MONEY, ON COMMAND", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, letterSpacing = 1.5.sp)
    Text("Just say the word.", style = MaterialTheme.typography.headlineMedium)
    Text("Tap the mic and say ‘Spent ₹400 on groceries’ or ‘What has my spending been like this month?’",
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 13.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(languageCode == "en-IN", onClick = { onAction(VoxAction.LanguageChanged("en-IN")) }, label = { Text("English") })
        FilterChip(languageCode == "hi-IN", onClick = { onAction(VoxAction.LanguageChanged("hi-IN")) }, label = { Text("हिन्दी / Hinglish") })
    }
    if (languageCode == "hi-IN") Text("Try: मेरा इस महीने का बजट 4000 सेट कर दीजिए",
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
    OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().padding(top = 8.dp),
        placeholder = { Text("Or type a transaction here...") },
        trailingIcon = { IconButton(onClick = { onAction(VoxAction.TypeInstead(text)) }) { Icon(VoxIcons.Forward, contentDescription = "Continue with typed transaction") } },
        singleLine = true, shape = RoundedCornerShape(14.dp))
    Text("✧  Your transaction appears instantly on your dashboard.", color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp, textAlign = TextAlign.Center)
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(icon, contentDescription = null) })
}

@Composable
private fun MicButton(scale: Float, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(90.dp).scale(scale),
    ) {
        Icon(VoxIcons.Mic, contentDescription = stringResource(R.string.cd_start_listening), modifier = Modifier.size(42.dp))
    }
}

@Composable
private fun ListeningContent(onAction: (VoxAction) -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = PULSE_SCALE,
        animationSpec = infiniteRepeatable(tween(PULSE_MS), RepeatMode.Reverse),
        label = "pulse-scale",
    )
    Spacer(Modifier.heightIn(min = VoxSpacing.ExtraLarge))
    Box(contentAlignment = Alignment.Center) { MicButton(scale = pulse, onClick = {}) }
    Text(
        stringResource(R.string.vox_listening),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    LinearProgressIndicator(Modifier.fillMaxWidth())
    OutlinedButton(onClick = { onAction(VoxAction.CancelListening) }) { Text(stringResource(R.string.cancel)) }
}

@Composable
private fun RecognizedContent(result: VoiceResult, onAction: (VoxAction) -> Unit) {
    Column(
        Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(VoxSpacing.Small),
    ) {
        Text(
            stringResource(R.string.vox_transcript, result.transcript),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider(Modifier.padding(vertical = VoxSpacing.Small))
        Text(stringResource(R.string.vox_found), style = MaterialTheme.typography.labelLarge)
        LabeledValue(stringResource(R.string.field_type), stringResource(result.type.labelRes))
        LabeledValue(stringResource(R.string.field_amount), LocalMoneyFormatter.current.format(result.amount))
        LabeledValue(stringResource(R.string.field_category), stringResource(result.category.labelRes))
        LabeledValue(stringResource(R.string.field_date), stringResource(R.string.date_today))
    }
    Button(
        onClick = { onAction(VoxAction.Confirm) },
        modifier = Modifier.fillMaxWidth().heightIn(min = VoxSize.MinTouchTarget),
    ) { Text(stringResource(R.string.confirm)) }
    TextButton(onClick = { onAction(VoxAction.EditDetails(result)) }) { Text(stringResource(R.string.vox_edit_details)) }
    TextButton(onClick = { onAction(VoxAction.StartListening) }) { Text(stringResource(R.string.vox_try_again)) }
}

@Preview
@Composable
private fun VoxIdlePreview() {
    VoxTheme { VoxScreen(VoxUiState.Idle, "en-IN") {} }
}

@Preview
@Composable
private fun VoxRecognizedPreview() {
    VoxTheme {
        VoxScreen(
            VoxUiState.Recognized(
                VoiceResult("Spent ₹400 on groceries", TransactionType.EXPENSE, PreviewFixtures.inr(400), "Groceries", Category.GROCERIES),
            ),
            "en-IN",
        ) {}
    }
}
