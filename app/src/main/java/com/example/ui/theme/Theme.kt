package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class KhataSemanticColors(
    val creditGreen: Color,
    val creditGreenContainer: Color,
    val debitRed: Color,
    val debitRedContainer: Color,
    val saffronGold: Color,
    val saffronGoldContainer: Color,
    val cardBorder: Color,
    val heroStart: Color,
    val heroEnd: Color
)

val LocalKhataColors = staticCompositionLocalOf {
    KhataSemanticColors(
        creditGreen = CreditEmerald,
        creditGreenContainer = CreditEmeraldContainer,
        debitRed = DebitCrimson,
        debitRedContainer = DebitCrimsonContainer,
        saffronGold = SaffronGold,
        saffronGoldContainer = SaffronGoldContainer,
        cardBorder = SoftSlateBorder,
        heroStart = DeepTeal,
        heroEnd = PeacockTeal
    )
}

private val LightColorScheme = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = SoftTealSurface,
    onPrimaryContainer = DeepTealDark,
    secondary = PeacockTeal,
    onSecondary = Color.White,
    secondaryContainer = CreditEmeraldContainer,
    onSecondaryContainer = CreditEmeraldContainerDark,
    tertiary = SaffronGold,
    onTertiary = Color.White,
    tertiaryContainer = SaffronGoldContainer,
    onTertiaryContainer = SaffronGoldContainerDark,
    background = WarmPaperBg,
    onBackground = InkSlate,
    surface = PureCardWhite,
    onSurface = InkSlate,
    surfaceVariant = Color(0xFFEEF2F0),
    onSurfaceVariant = MutedSlate,
    outline = SoftSlateBorder,
    error = DebitCrimson,
    onError = Color.White,
    errorContainer = DebitCrimsonContainer,
    onErrorContainer = DebitCrimsonContainerDark
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FD1C5),
    onPrimary = DeepTealDark,
    primaryContainer = DeepTeal,
    onPrimaryContainer = Color(0xFFE6FFFA),
    secondary = CreditEmeraldDark,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = CreditEmeraldContainerDark,
    onSecondaryContainer = CreditEmeraldContainer,
    tertiary = SaffronGoldDark,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = SaffronGoldContainerDark,
    onTertiaryContainer = SaffronGoldContainer,
    background = MidnightBg,
    onBackground = FrostText,
    surface = ObsidianCard,
    onSurface = FrostText,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = FrostMuted,
    outline = DarkBorder,
    error = DebitCrimsonDark,
    onError = Color(0xFF450A0A),
    errorContainer = DebitCrimsonContainerDark,
    onErrorContainer = DebitCrimsonContainer
)

val KhataShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun KhataMitraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) {
        KhataSemanticColors(
            creditGreen = CreditEmeraldDark,
            creditGreenContainer = CreditEmeraldContainerDark,
            debitRed = DebitCrimsonDark,
            debitRedContainer = DebitCrimsonContainerDark,
            saffronGold = SaffronGoldDark,
            saffronGoldContainer = SaffronGoldContainerDark,
            cardBorder = DarkBorder,
            heroStart = DeepTealDark,
            heroEnd = Color(0xFF115E59)
        )
    } else {
        KhataSemanticColors(
            creditGreen = CreditEmerald,
            creditGreenContainer = CreditEmeraldContainer,
            debitRed = DebitCrimson,
            debitRedContainer = DebitCrimsonContainer,
            saffronGold = SaffronGold,
            saffronGoldContainer = SaffronGoldContainer,
            cardBorder = SoftSlateBorder,
            heroStart = DeepTeal,
            heroEnd = PeacockTeal
        )
    }

    CompositionLocalProvider(LocalKhataColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KhataTypography,
            shapes = KhataShapes,
            content = content
        )
    }
}
