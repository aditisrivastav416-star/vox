package com.example.vox.feature.receipt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.core.designsystem.component.VoxBrand
import androidx.compose.ui.text.font.FontWeight
import com.example.vox.core.designsystem.icon.VoxIcons
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.ui.ObserveEvents

@Composable
fun ReceiptScannerRoute(viewModel: ReceiptScannerViewModel, onBack: () -> Unit, onScanned: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { if (it is ReceiptScannerEvent.Scanned) onScanned(it.receiptId) }
    ReceiptScannerScreen(state, viewModel::onAction, onBack)
}

@Composable
fun ReceiptScannerScreen(state: ReceiptScannerUiState, onAction: (ReceiptScannerAction) -> Unit, onBack: () -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAction(ReceiptScannerAction.ImagePicked(uri.toString()))
    }
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
            .padding(horizontal = 23.dp).padding(top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("EFFORTLESS EXPENSES", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, letterSpacing = 1.5.sp,
            modifier = Modifier)
        Text("Scan a receipt ✳", color = ink, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Text("From paper to perfectly organized in a snap.", color = muted, fontSize = 14.sp)
        Box(
            Modifier.fillMaxWidth().height(280.dp).padding(top = 14.dp)
                .background(Brush.radialGradient(listOf(Color(0xFFECE8FF), MaterialTheme.colorScheme.background)), RoundedCornerShape(25.dp))
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(25.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(220.dp).border(1.dp, Color(0xFFCCC4FF), RoundedCornerShape(110.dp)))
            Column(
                Modifier.size(width = 145.dp, height = 190.dp).rotate(-10f)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(13.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VoxBrand(iconSize = 20.dp)
                Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xFFE4E4ED), RoundedCornerShape(4.dp)))
                Box(Modifier.fillMaxWidth(.7f).height(5.dp).background(Color(0xFFE4E4ED), RoundedCornerShape(4.dp)))
                Box(Modifier.fillMaxWidth(.5f).height(5.dp).background(Color(0xFFE4E4ED), RoundedCornerShape(4.dp)))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", color = ink, fontSize = 9.sp)
                    Text("$ 00.00", color = ink, fontSize = 9.sp)
                }
            }
            Text("✧ VOX SCAN", color = Color.White, fontSize = 10.sp, letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp).rotate(9f)
                    .background(Color(0xFF8061EF), RoundedCornerShape(8.dp)).padding(10.dp))
        }
        if (state.isScanning) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        ActionCard("Take a photo", "Capture with your camera", VoxIcons.Camera, true, !state.isScanning) {
            onAction(ReceiptScannerAction.Capture)
        }
        ActionCard("Upload from gallery", "Choose an existing photo", VoxIcons.Gallery, false, !state.isScanning) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        Text(state.error?.asString() ?: "✧  A little magic, a lot less effort.\n     We'll help turn your receipt into an organized expense.",
            color = if (state.error != null) MaterialTheme.colorScheme.error else muted,
            fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(14.dp)).padding(15.dp).semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (primary) Brush.linearGradient(listOf(Color(0xFF5444C7), Color(0xFF8C6AF3))) else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerLow))
    Row(Modifier.fillMaxWidth().height(78.dp).background(bg, RoundedCornerShape(18.dp)).clickable(enabled = enabled, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, tint = if (primary) Color.White else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(46.dp).background(if (primary) Color.White.copy(alpha = .15f) else MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(12.dp)).padding(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (primary) Color.White else MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
            Text(subtitle, color = if (primary) Color.White.copy(alpha = .75f) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Text("→", color = if (primary) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview
@Composable
private fun ReceiptScannerPreview() { VoxTheme { ReceiptScannerScreen(ReceiptScannerUiState(), {}, {}) } }
