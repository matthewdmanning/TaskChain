package dev.augmentedui.generator.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Immutable color and component tokens for one cyberpunk theme. */
@Immutable
data class CyberTheme(
    val cyanGlow: Color,
    val magentaGlow: Color,
    val surfaceDark: Color,
    val surfaceRaised: Color,
    val surfacePressed: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val disabledSurface: Color,
    val disabledContent: Color,
    val typography: CyberTypography = CyberTypography(),
    val spacing: CyberSpacing = CyberSpacing(),
)

/** Monospaced text roles used by cyberpunk components without Material typography. */
@Immutable
data class CyberTypography(
    val label: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    val telemetry: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
)

/** Constrained spacing scale shared by the standalone component library. */
@Immutable
data class CyberSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 16.dp,
    val md: Dp = 24.dp,
    val lg: Dp = 32.dp,
    val xl: Dp = 48.dp,
    val xxl: Dp = 64.dp,
)

val DefaultCyberTheme = CyberTheme(
    cyanGlow = Color(0xFF00F0FF),
    magentaGlow = Color(0xFFFF2BD6),
    surfaceDark = Color(0xFF080B12),
    surfaceRaised = Color(0xFF111827),
    surfacePressed = Color(0xFF17243A),
    textPrimary = Color(0xFFF4FBFF),
    textMuted = Color(0xFF8FA5B8),
    disabledSurface = Color(0xFF202733),
    disabledContent = Color(0xFF697586),
)

val LocalCyberTheme = staticCompositionLocalOf { DefaultCyberTheme }

/** Use this function at a UI boundary so every cyberpunk component reads the same tokens. */
@Composable
fun CyberThemeProvider(
    theme: CyberTheme = DefaultCyberTheme,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCyberTheme provides theme, content = content)
}
