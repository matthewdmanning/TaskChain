package com.taskchain.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.MaterialTheme
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

/** Hue step between neighbouring subtask colors; a prime so far-apart indexes stay distinct. */
private const val SUBTASK_HUE_STEP_DEGREES = 47f

private const val FULL_HUE_DEGREES = 360f

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

/** Semantic layout spacing shared across UI providers. */
data class Spacing(
    val small: androidx.compose.ui.unit.Dp,
    val medium: androidx.compose.ui.unit.Dp,
    val large: androidx.compose.ui.unit.Dp,
)

/** Provider-independent semantic tokens resolved at the selected theme boundary. */
data class TaskChainTokens(
    val spacing: Spacing,
    val subtaskColorSeed: Color,
)

/** Default tokens for previews and content without an installed app theme. */
private val DefaultTaskChainTokens = TaskChainTokens(
    spacing = Spacing(CyberPrimitives.Spacing.dp8, CyberPrimitives.Spacing.dp16, CyberPrimitives.Spacing.dp24),
    subtaskColorSeed = CyberPrimitives.Colors.Cyan500,
)

/** Current semantic token mapping; provider-specific values are resolved by the theme. */
val LocalTaskChainTokens = compositionLocalOf { DefaultTaskChainTokens }

/** Semantic tokens consumed by feature UI without direct Cyberpunk theme access. */
object TaskChainDesignSystem {
    /** Finger travel along a drag handle that moves a task or subtask one place. */
    val reorderDragDistance = 48.dp

    /** Seed for the stable ordered subtask palette. */
    val subtaskColorSeed: Color
        @Composable get() = LocalTaskChainTokens.current.subtaskColorSeed

    /**
     * Returns a stable palette color for an ordered subtask.
     *
     * @param index Zero-based subtask position.
     */
    @Composable
    fun subtaskColor(index: Int): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(subtaskColorSeed.toArgb(), hsv)
        return Color.hsv((hsv[0] + index * SUBTASK_HUE_STEP_DEGREES) % FULL_HUE_DEGREES, hsv[1], hsv[2])
    }

    /** Returns semantic spacing from the currently selected UI provider. */
    @Composable
    fun spacing(): Spacing = LocalTaskChainTokens.current.spacing

    /*
     * Feature UI styles only through the tokens below and the members above. Features never read
     * cyberpunkAndroid's CyberTheme or CyberPrimitives, or MaterialTheme.typography, directly;
     * scripts/check-feature-boundaries.py enforces this. Each token currently returns the exact library or
     * Material value features used before, so a library change reaches features only through this boundary.
     */

    /** Gap and padding scale on the shared grid. Non-composable, so it is usable inside draw scopes. */
    object Space {
        /** Tight inset around a card inside a list. */
        val extraSmall: Dp = CyberPrimitives.Spacing.dp4

        /** Gap between related items, such as lines in a list or controls in a row. */
        val small: Dp = CyberPrimitives.Spacing.dp8

        /** Padding inside compact rows and gaps between status dots. */
        val smallMedium: Dp = CyberPrimitives.Spacing.dp12

        /** Content padding of screens, cards and prominent controls. */
        val medium: Dp = CyberPrimitives.Spacing.dp16

        /** Separation between major blocks of a screen. */
        val large: Dp = CyberPrimitives.Spacing.dp24

        /** Generous separation, such as clearance below a floating action. */
        val extraLarge: Dp = CyberPrimitives.Spacing.dp32
    }

    /** Component sizes. Non-composable, so they are usable inside draw scopes. */
    object Sizes {
        /** Minimum width or height of any tappable control. */
        val touchTarget: Dp = CyberPrimitives.IconSizes.dp48

        /** Minimum height of the single most important action on a screen. */
        val prominentTouchTarget: Dp = CyberPrimitives.IconSizes.dp64

        /** Standalone icon that carries meaning by itself, such as a drag handle or status symbol. */
        val iconLarge: Dp = CyberPrimitives.IconSizes.dp48

        /** Icon marking the current item in a compact status row. */
        val iconMedium: Dp = CyberPrimitives.IconSizes.dp24

        /** Icon marking a non-current item in a compact status row. */
        val iconSmall: Dp = CyberPrimitives.IconSizes.dp16

        /** Icon placed beside a button label. */
        val inlineIcon: Dp = CyberPrimitives.Spacing.dp16

        /** Smallest width of a wrapping action button at default font scale; callers scale it by font scale. */
        val actionMinWidth: Dp = 112.dp

        /** Placeholder height that keeps an empty list section tall enough to read as a section. */
        val emptySectionHeight: Dp = CyberPrimitives.IconSizes.dp48 + CyberPrimitives.Spacing.dp32

        /** Glow spread around a highlighted card border. */
        val cardGlowRadius: Dp = CyberPrimitives.Spacing.dp8
    }

    /** Border and outline widths. Non-composable, so they are usable inside draw scopes. */
    object Strokes {
        /** Default outline of cards, rows and inactive status markers. */
        val hairline: Dp = CyberPrimitives.BorderWidths.dp1

        /** Outline of the selected or current item. */
        val emphasis: Dp = CyberPrimitives.BorderWidths.dp2

        /** Outline of the primary completion action. */
        val primaryActionBorder: Dp = 2.dp

        /** Outline of a destructive confirmation dialog. */
        val alertBorder: Dp = 3.dp
    }

    /** Geometry of the runner countdown dial and its subtask rims. Non-composable, so usable in draw scopes. */
    object Dial {
        /** Smallest dial diameter on tiny windows. */
        val minDiameter: Dp = CyberPrimitives.IconSizes.dp48

        /** Space kept between the dial and each screen edge. */
        val screenInset: Dp = CyberPrimitives.Spacing.dp32

        /** Thickness of the segmented outer rim and of the tick strokes. */
        val rimThickness: Dp = CyberPrimitives.BorderWidths.dp2 * 3

        /** Length of a minor tick. */
        val tickLength: Dp = CyberPrimitives.Spacing.dp12

        /** Length of a major tick. */
        val majorTickLength: Dp = CyberPrimitives.Spacing.dp24

        /** Width of the sweep hand that follows elapsed time. */
        val sweepStroke: Dp = 2.dp

        /** Glow spread around the timer digits while time remains comfortable. */
        val timerGlowRadius: Dp = CyberPrimitives.Spacing.dp8

        /** Glow spread around the timer digits once time is urgent. */
        val urgentTimerGlowRadius: Dp = CyberPrimitives.Spacing.dp16

        /** Radial space reserved for each subtask rim, including the gap to its neighbour. */
        val subtaskRimSlot: Dp = CyberPrimitives.Spacing.dp8

        /** Thickest subtask rim stroke. */
        val subtaskRimMaxStroke: Dp = 3.dp
    }

    /** Color roles resolved from the installed theme. */
    object Colors {
        /** Main accent for focus, dragging and highlights. */
        val primary: Color
            @Composable get() = CyberTheme.colors.primary

        /** Secondary accent for titles and back navigation. */
        val secondary: Color
            @Composable get() = CyberTheme.colors.secondary

        /** Screen background. */
        val background: Color
            @Composable get() = CyberTheme.colors.background

        /** Surface of editable rows. */
        val surface: Color
            @Composable get() = CyberTheme.colors.surface

        /** Surface of raised cards. */
        val surfaceSecondary: Color
            @Composable get() = CyberTheme.colors.surfaceSecondary

        /** Main text and icon content. */
        val textPrimary: Color
            @Composable get() = CyberTheme.colors.textPrimary

        /** Supporting text, placeholders and inactive icons. */
        val textSecondary: Color
            @Composable get() = CyberTheme.colors.textSecondary

        /** Outlines and neutral dial structure. */
        val border: Color
            @Composable get() = CyberTheme.colors.border

        /** Completed or successful state. */
        val success: Color
            @Composable get() = CyberTheme.semantics.colors.success

        /** Attention state: skipped, paused, behind schedule or nearing the deadline. */
        val warning: Color
            @Composable get() = CyberTheme.semantics.colors.warning

        /** Failure state: overtime or destructive confirmation. */
        val error: Color
            @Composable get() = CyberTheme.semantics.colors.error

        /** Invalid input and validation messages. */
        val danger: Color
            @Composable get() = CyberTheme.semantics.colors.danger

        /** Neutral in-progress information. */
        val info: Color
            @Composable get() = CyberTheme.semantics.colors.info

        /** Shade mixed into a color to darken it, such as the body of a flash icon. */
        val shade: Color = Color.Black
    }

    /** Shapes resolved from the installed theme. */
    object Shapes {
        /** Cut-corner outline of cards and dialogs. */
        val panel: Shape
            @Composable get() = CyberTheme.shapes.cyberCutCornerShape

        /** Smaller cut-corner outline of list rows. */
        val compactPanel: Shape
            @Composable get() = CyberTheme.shapes.cyberCutCornerShapeSmall
    }

    /**
     * The single text-style system for feature UI. Theme-specific styles and the Material type scale configured in
     * [TaskChainTheme] are both reached through these roles, so features never mix typography sources.
     */
    object TextStyles {
        /** Screen and section titles. */
        val display: TextStyle
            @Composable get() = CyberTheme.typography.display

        /** Running text and list item labels. */
        val body: TextStyle
            @Composable get() = CyberTheme.typography.body

        /** Small monospaced labels and timestamps. */
        val terminalLabel: TextStyle
            @Composable get() = CyberTheme.typography.terminal

        /** Most prominent text on a screen, such as the current runner task. */
        val hero: TextStyle
            @Composable get() = MaterialTheme.typography.displayMedium

        /** Page or dialog heading. */
        val heading: TextStyle
            @Composable get() = MaterialTheme.typography.headlineLarge

        /** Heading beneath the hero text, such as the current subtask. */
        val subheading: TextStyle
            @Composable get() = MaterialTheme.typography.headlineMedium

        /** Label of the primary action. */
        val actionLabel: TextStyle
            @Composable get() = MaterialTheme.typography.headlineSmall

        /** Section titles and emphasized supporting text. */
        val title: TextStyle
            @Composable get() = MaterialTheme.typography.titleLarge
    }
}

/**
 * Use this function at the app boundary so all colors, typography, fonts, shapes, and Material mappings originate
 * from cyberpunkAndroid while retaining the user's System/Light/Dark preference.
 *
 * @param selectedTheme Persisted theme option ID from [ThemeCatalog].
 * @param selectedUiProvider Persisted component provider ID; Material receives an independent Material 3 overlay.
 * @param content Application content rendered inside [CyberTheme], preserving legacy Cyber tokens during migration.
 */
@Composable
fun TaskChainTheme(selectedTheme: String, selectedUiProvider: String = UiProvider.CYBERPUNK.id, content: @Composable () -> Unit) {
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
        bodySmall = base.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
        bodyMedium = base.bodyLarge.copy(fontSize = 22.sp, lineHeight = 30.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 22.sp, lineHeight = 30.sp),
        labelSmall = base.labelLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
        labelMedium = base.labelLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
        labelLarge = base.labelLarge.copy(fontSize = 22.sp, lineHeight = 30.sp),
        titleSmall = base.titleSmall.copy(fontSize = 22.sp, lineHeight = 30.sp),
        titleMedium = base.titleMedium.copy(fontSize = 24.sp, lineHeight = 32.sp),
        headlineSmall = base.headlineSmall.copy(fontSize = 28.sp, lineHeight = 36.sp),
        headlineMedium = base.headlineMedium.copy(fontSize = 32.sp, lineHeight = 40.sp),
        displayMedium = base.displayMedium.copy(fontSize = 48.sp, lineHeight = 56.sp),
        titleLarge = base.headlineLarge,
    ))
    CompositionLocalProvider(LocalDensity provides Density(density.density, cappedScale)) {
        CyberTheme(
            colors = colors,
            typography = CyberTypography(display = CyberTypography().display.copy(fontSize = 40.sp),
                terminal = CyberTypography().terminal.copy(fontSize = 22.sp, lineHeight = 30.sp),
                body = CyberTypography().body.copy(fontSize = 22.sp, lineHeight = 30.sp)),
            materialTypography = typography,
            content = {
                val provider = UiProvider.fromId(selectedUiProvider)
                if (provider == UiProvider.MATERIAL) {
                    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                        CompositionLocalProvider(
                            LocalTaskChainTokens provides TaskChainTokens(
                                spacing = Spacing(
                                    CyberPrimitives.Spacing.dp8,
                                    CyberPrimitives.Spacing.dp16,
                                    CyberPrimitives.Spacing.dp24,
                                ),
                                subtaskColorSeed = MaterialTheme.colorScheme.tertiary,
                            ),
                            content = content,
                        )
                    }
                } else {
                    CompositionLocalProvider(
                        LocalTaskChainTokens provides TaskChainTokens(
                            spacing = DefaultTaskChainTokens.spacing,
                            subtaskColorSeed = CyberTheme.semantics.colors.info,
                        ),
                        content = content,
                    )
                }
            },
        )
    }
}
