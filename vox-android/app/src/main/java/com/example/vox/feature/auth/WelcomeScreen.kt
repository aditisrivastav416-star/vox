package com.example.vox.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vox.core.designsystem.component.VoxBrand
import com.example.vox.core.designsystem.theme.VoxTheme
import com.example.vox.core.designsystem.theme.VoxAppearance
import com.example.vox.core.designsystem.theme.VoxDisplayFont

@Composable
fun WelcomeScreen(onContinueWithEmail: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val dark = VoxAppearance.dark
    val context = LocalContext.current
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 27.dp).padding(top = 24.dp, bottom = 22.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            VoxBrand()
            IconButton(onClick = { VoxAppearance.toggle(context) }, modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(13.dp))) {
                Icon(if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, contentDescription = "Switch appearance", tint = ink)
            }
        }
        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(185.dp).border(1.dp, Color(0x55AAA2ED), RoundedCornerShape(100.dp)))
            Box(Modifier.size(120.dp).border(1.dp, Color(0x55AAA2ED), RoundedCornerShape(100.dp)))
            Box(
                Modifier.width(207.dp).height(130.dp).rotate(17f).background(
                    Brush.linearGradient(listOf(Color(0xFF6EA5ED), Color(0xFF4C65CE), Color(0xFF362D8C))), RoundedCornerShape(17.dp)
                )
            )
            Column(
                Modifier.width(207.dp).height(130.dp).rotate(-12f).background(
                    Brush.linearGradient(listOf(Color(0xFF372E81), Color(0xFF7062D9), Color(0xFFBAA3FB))), RoundedCornerShape(17.dp)
                ).padding(19.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                VoxBrand(light = true, iconSize = 25.dp)
                Text("MAKE MONEY MAKE SENSE", fontSize = 6.sp, letterSpacing = 1.4.sp, color = Color(0xFFE6E2FF))
                Box(Modifier.width(29.dp).height(21.dp).background(Color(0xFFE9D397), RoundedCornerShape(4.dp)))
            }
            Text("✳", modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp).size(47.dp)
                .background(Color(0xFF9C86E5), RoundedCornerShape(50)).padding(top = 5.dp), textAlign = TextAlign.Center,
                color = Color.White, fontSize = 28.sp)
        }
        Text("WELCOME TO VOX", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.55.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        Text("Money, meet", fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-2).sp, fontFamily = VoxDisplayFont, color = ink, modifier = Modifier.semantics { heading() })
        Text("clarity.", fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-2).sp, fontFamily = VoxDisplayFont, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(3.dp))
        Text("Everything you spend, split, and save. Beautifully in sync.", fontSize = 12.sp, lineHeight = 19.sp, color = muted, modifier = Modifier.width(270.dp))
        Spacer(Modifier.height(14.dp))
        Text("Email address", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ink)
        Spacer(Modifier.height(2.dp))
        OutlinedTextField(email, { email = it; showError = false }, Modifier.fillMaxWidth().height(52.dp), placeholder = { Text("you@example.com", fontSize = 12.sp) },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow))
        Spacer(Modifier.height(10.dp))
        Text("Password", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ink)
        Spacer(Modifier.height(2.dp))
        OutlinedTextField(password, { password = it; showError = false }, Modifier.fillMaxWidth().height(52.dp), placeholder = { Text("At least 4 characters", fontSize = 12.sp) },
            singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow))
        if (showError) Text("Enter a valid email and at least 4 characters.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        Spacer(Modifier.height(18.dp))
        Button(onClick = { if (email.contains('@') && password.length >= 4) onContinueWithEmail() else showError = true },
            modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750DD))) {
            Text("Continue with email", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(23.dp))
        Text("Your finances, your space. No real bank connection required.", modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, color = muted, fontSize = 10.sp)
    }
}

@Preview
@Composable
private fun WelcomeScreenPreview() { VoxTheme { WelcomeScreen({}) } }
