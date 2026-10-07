package com.taskchain.ui.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.Typography
import com.example.cyberpunkandroid.theme.CyberTypography
import com.example.cyberpunkandroid.theme.cyberMaterialTypography
import androidx.compose.ui.platform.LocalContext
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.theme.CyberColors
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.R

/** Theme choices exposed to settings without assuming every theme has every variant. */
data class ThemeOption(
    val id: String,
    @StringRes val label: Int,
    val supportedModes: Set<ThemeMode>,
)

/** Appearance variants a theme may choose to support. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Central catalog consumed by settings and the app theme boundary. */
object ThemeCatalog {
    /** Use this function when a caller needs the configured appearance choices. */
    fun options(context: android.content.Context): List<ThemeOption> {
        val resources = context.resources
        val ids = resources.getStringArray(R.array.theme_option_ids)
        val labels = resources.obtainTypedArray(R.array.theme_option_labels)
        val modes = resources.getStringArray(R.array.theme_option_modes)
        return try {
            ids.indices.map { index ->
                ThemeOption(
                    id = ids[index],
                    label = labels.getResourceId(index, 0),
                    supportedModes = modes[index].split(',').map { ThemeMode.valueOf(it.trim().uppercase()) }.toSet(),
                )
            }
        } finally {
            labels.recycle()
        }
    }
}

/** Semantic spacing tokens backed directly by cyberpunkAndroid primitives. */
data class Spacing(
    val small: androidx.compose.ui.unit.Dp,
    val medium: androidx.compose.ui.unit.Dp,
    val large: androidx.compose.ui.unit.Dp,
)

/** Provides configured spacing to feature UI through the design-system boundary. */
object TaskChainDesignSystem {
    /** Semantic seed for the ordered cue palette; configure its theme mapping here. */
    val cueColorSeed: Color
        @Composable get() = CyberTheme.semantics.colors.info

    /** Use this function to generate a stable sequential cue color from the design-system seed.
     * Inputs: ordered cue index. Dependencies: cueColorSeed and native HSV color conversion.
     */
    @Composable
    fun cueColor(index: Int): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(cueColorSeed.toArgb(), hsv)
        return Color.hsv((hsv[0] + index * 47f) % 360f, hsv[1], hsv[2])
    }

    /** Use this function when feature UI needs semantic spacing from cyberpunkAndroid. */
    @Composable
    fun spacing(): Spacing = Spacing(
        small = CyberPrimitives.Spacing.dp8,
        medium = CyberPrimitives.Spacing.dp16,
        large = CyberPrimitives.Spacing.dp24,
    )
}

/**
 * Use this function at the app boundary so all colors, typography, fonts, shapes, and Material mappings originate
 * from cyberpunkAndroid while retaining the user's System/Light/Dark preference.
 *
 * @param selectedTheme Persisted theme option ID from [ThemeCatalog].
 * @param content Application content rendered inside [CyberTheme].
 */
@Composable
fun TaskChainTheme(selectedTheme: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val options = remember(context) { ThemeCatalog.options(context) }
    val selected = options.firstOrNull { it.id == selectedTheme }
    val modes = selected?.supportedModes.orEmpty()
    val dark = when {
        ThemeMode.SYSTEM in modes -> isSystemInDarkTheme()
        ThemeMode.DARK in modes && ThemeMode.LIGHT !in modes -> true
        ThemeMode.LIGHT in modes && ThemeMode.DARK !in modes -> false
        else -> isSystemInDarkTheme()
    }
    val palette = CyberPrimitives.Colors
    val colors = if (dark) {
        CyberColors(
            primary = palette.Cyan500,
            secondary = palette.Magenta500,
            background = palette.Void500,
            surface = palette.Void100,
            surfacePrimary = palette.Void500,
            surfaceSecondary = palette.Void200,
            surfaceTertiary = palette.Void100,
            surfaceElevated = palette.Void200,
            textPrimary = palette.Chrome100,
            textSecondary = palette.Chrome300,
            border = palette.Chrome600,
        )
    } else {
        CyberColors(
            primary = palette.Cyan500,
            secondary = palette.Magenta500,
            background = palette.Chrome100,
            surface = palette.Chrome200,
            surfacePrimary = palette.Chrome100,
            surfaceSecondary = palette.Chrome200,
            surfaceTertiary = palette.Chrome300,
            surfaceElevated = palette.Chrome100,
            textPrimary = palette.Void500,
            textSecondary = palette.Chrome600,
            border = palette.Chrome600,
        )
    }
    val density = LocalDensity.current
    val width = LocalConfiguration.current.screenWidthDp
    // Bound enlarged text by the available header width while retaining the user's scale below that limit.
    val cappedScale = density.fontScale.coerceAtMost((width / 240f).coerceAtLeast(1f))
    val base = Typography()
    val typography = cyberMaterialTypography(base.copy(
        bodySmall = base.bodyLarge,
        bodyMedium = base.bodyLarge,
        labelSmall = base.labelLarge,
        labelMedium = base.labelLarge,
        titleLarge = base.headlineLarge,
    ))
    CompositionLocalProvider(LocalDensity provides Density(density.density, cappedScale)) {
        CyberTheme(
            colors = colors,
            typography = CyberTypography(display = CyberTypography().display.copy(fontSize = 40.sp),
                terminal = CyberTypography().terminal.copy(fontSize = 18.sp),
                body = CyberTypography().body.copy(fontSize = 18.sp)),
            materialTypography = typography,
            content = content,
        )
    }
}
