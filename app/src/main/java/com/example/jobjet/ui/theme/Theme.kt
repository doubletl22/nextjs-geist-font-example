package com.example.jobjet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2962FF),
    secondary = Color(0xFFFF6E40),
    background = Color(0xFF001133)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2962FF),
    secondary = Color(0xFFFF6E40),
    background = Color(0xFFFFFFFF)
)

@Composable
fun JobJetTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
