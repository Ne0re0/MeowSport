package fr.meowsport.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import fr.meowsport.Accent
import fr.meowsport.ThemeMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Violet = Color(0xFFB69CFF)
val Gold = Color(0xFFFFC94D)

private val Dark = darkColorScheme(
    primary = Violet,
    onPrimary = Color(0xFF22104F),
    primaryContainer = Color(0xFF3D2A73),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Gold,
    background = Color(0xFF17151A),
    onBackground = Color(0xFFE9E4EC),
    surface = Color(0xFF17151A),
    onSurface = Color(0xFFE9E4EC),
    surfaceVariant = Color(0xFF2B2830),
    onSurfaceVariant = Color(0xFFCAC3CF),
    surfaceContainerLowest = Color(0xFF121014),
    surfaceContainerLow = Color(0xFF1E1B21),
    surfaceContainer = Color(0xFF221F26),
    surfaceContainerHigh = Color(0xFF2B2830),
    surfaceContainerHighest = Color(0xFF36323B),
    outline = Color(0xFF948E99),
    outlineVariant = Color(0xFF49454E),
)

private val Light = lightColorScheme(
    primary = Color(0xFF6D3FD8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF8A6100),
    background = Color(0xFFFDF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

val ClockStyle = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

@Composable
fun MeowSportTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accent: Accent = Accent.VIOLET,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val base = if (dark) Dark else Light
    // The accent setting replaces the primary colors; violet is the default look.
    val scheme = if (accent == Accent.VIOLET) base else {
        val p = Color(if (dark) accent.dark else accent.light)
        base.copy(
            primary = p,
            onPrimary = if (dark) Color(0xFF1A1A1A) else Color.White,
            primaryContainer = lerp(p, base.background, if (dark) 0.65f else 0.8f),
            onPrimaryContainer = if (dark) lerp(p, Color.White, 0.75f) else lerp(p, Color.Black, 0.6f),
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content,
    )
}
