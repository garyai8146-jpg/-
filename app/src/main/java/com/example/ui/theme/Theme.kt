package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
      primary = PrimaryBlue,
      onPrimary = TextLight,
      primaryContainer = PrimaryLight,
      onPrimaryContainer = TextDark,
      secondary = AccentGreen,
      background = BgDark,
      onBackground = TextLight,
      surface = SurfaceDark,
      onSurface = TextLight,
      surfaceVariant = SurfaceVariantDark,
      onSurfaceVariant = TextGray,
      tertiaryContainer = SurfaceVariantDarker,
      onTertiaryContainer = TextLight,
      errorContainer = AccentYellow
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
