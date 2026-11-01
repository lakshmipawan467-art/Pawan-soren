package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = OnForestGreen,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    secondary = EmeraldSecondary,
    onSecondary = OnEmerald,
    secondaryContainer = LightGreenContainer,
    onSecondaryContainer = OnLightGreenContainer,
    tertiary = GoldenTreasury,
    onTertiary = Color.White,
    tertiaryContainer = GoldenContainer,
    onTertiaryContainer = OnGoldenContainer,
    background = VillageBackground,
    onBackground = TextPrimary,
    surface = VillageSurface,
    onSurface = TextPrimary,
    surfaceVariant = VillageSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = VillageOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003A08),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF003811),
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFFFFD54F),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF704000),
    onTertiaryContainer = Color(0xFFFFE082),
    background = Color(0xFF111812),
    onBackground = Color(0xFFE2EBE2),
    surface = Color(0xFF182219),
    onSurface = Color(0xFFE2EBE2),
    surfaceVariant = Color(0xFF243326),
    onSurfaceVariant = Color(0xFFC4D5C5),
    outline = Color(0xFF425644)
)

@Composable
fun GaonFundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
