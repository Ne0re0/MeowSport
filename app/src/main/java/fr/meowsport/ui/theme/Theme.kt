package fr.meowsport.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Orange = Color(0xFFFF8A3D)
val Gold = Color(0xFFFFC94D)

private val Dark = darkColorScheme(
    primary = Orange,
    onPrimary = Color(0xFF2A1300),
    primaryContainer = Color(0xFF5A2A00),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFB9A4FF),
    onSecondary = Color(0xFF241458),
    secondaryContainer = Color(0xFF3A2E66),
    onSecondaryContainer = Color(0xFFE7DEFF),
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
    primary = Color(0xFFB65400),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF341100),
    secondary = Color(0xFF5B4BA8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7DEFF),
    onSecondaryContainer = Color(0xFF170B4A),
    tertiary = Color(0xFF8A6100),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF4DED3),
    onSurfaceVariant = Color(0xFF52443C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF1EA),
    surfaceContainer = Color(0xFFFCEAE1),
    surfaceContainerHigh = Color(0xFFF6E4DB),
    surfaceContainerHighest = Color(0xFFF0DFD5),
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
fun MeowSportTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}
