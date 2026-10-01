package com.example.vox.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vox.core.designsystem.theme.VoxAppearance

@Composable
fun VoxAppBar(profileName: String, onOpenProfile: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 27.dp, end = 27.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VoxBrand(Modifier.weight(1f))
        IconButton(
            onClick = { VoxAppearance.toggle(context) },
            modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(13.dp)),
        ) {
            Icon(
                if (VoxAppearance.dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = "Switch appearance",
            )
        }
        IconButton(onClick = onOpenProfile, modifier = Modifier.size(38.dp)) {
            Avatar(
                profileName.ifBlank { "A" },
                size = 38.dp,
                modifier = Modifier.semantics { contentDescription = "Open profile" },
            )
        }
    }
}
