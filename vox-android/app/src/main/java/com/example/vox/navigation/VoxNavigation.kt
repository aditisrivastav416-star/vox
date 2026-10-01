package com.example.vox.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vox.R
import com.example.vox.app.VoxApplication
import com.example.vox.core.designsystem.component.VoxAppBar
import com.example.vox.core.designsystem.icon.VoxIcons

private val topLevelRoutes = setOf(VoxDestination.HOME, VoxDestination.RECEIPT_SCAN, VoxDestination.ACTIVITY)

@Composable
fun VoxApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val app = LocalContext.current.applicationContext as VoxApplication
    val profile by app.container.userRepository.observeProfile().collectAsStateWithLifecycle(initialValue = null)
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (currentRoute in topLevelRoutes) {
                VoxAppBar(profile?.name.orEmpty()) { navController.navigate(VoxDestination.SETTINGS) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            VoxNavHost(navController, Modifier.fillMaxSize())
            if (currentRoute in topLevelRoutes) {
                VoxBottomBar(
                    currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(VoxDestination.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onVox = { navController.navigate(VoxDestination.VOX) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun VoxBottomBar(currentRoute: String?, onTabSelected: (String) -> Unit, onVox: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.weight(1f).height(62.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(19.dp)).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomTab(VoxIcons.Home, stringResource(R.string.nav_home), currentRoute == VoxDestination.HOME, Modifier.weight(1f)) { onTabSelected(VoxDestination.HOME) }
            BottomTab(VoxIcons.Scan, "Scan", currentRoute == VoxDestination.RECEIPT_SCAN, Modifier.weight(1f)) { onTabSelected(VoxDestination.RECEIPT_SCAN) }
            BottomTab(VoxIcons.Activity, stringResource(R.string.nav_activity), currentRoute == VoxDestination.ACTIVITY, Modifier.weight(1f)) { onTabSelected(VoxDestination.ACTIVITY) }
        }
        Box(
            Modifier.size(56.dp).background(
                Brush.linearGradient(listOf(Color(0xFF9A7CFF), Color(0xFF6246D7), Color(0xFF31379B))), RoundedCornerShape(19.dp)
            ).clickable(onClick = onVox), contentAlignment = Alignment.Center,
        ) { Icon(VoxIcons.Mic, contentDescription = stringResource(R.string.cd_open_vox), tint = Color.White) }
    }
}

@Composable
private fun BottomTab(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    androidx.compose.foundation.layout.Column(
        modifier.fillMaxHeight().background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
        Text(label, color = color, fontSize = 10.sp, lineHeight = 12.sp)
    }
}
