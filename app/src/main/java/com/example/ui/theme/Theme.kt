package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldLight,
  onPrimary = Slate900,
  primaryContainer = EmeraldDark,
  onPrimaryContainer = Emerald80,
  secondary = IndigoAccent,
  onSecondary = Color.White,
  background = Slate900,
  surface = Slate800,
  onBackground = Color.White,
  onSurface = Slate100,
  error = CoralError
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  onPrimary = Color.White,
  primaryContainer = Emerald80,
  onPrimaryContainer = EmeraldDark,
  secondary = IndigoAccent,
  onSecondary = Color.White,
  background = Slate50,
  surface = Color.White,
  onBackground = Slate900,
  onSurface = Slate800,
  error = CoralError
)

@Composable
fun MyShopTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
