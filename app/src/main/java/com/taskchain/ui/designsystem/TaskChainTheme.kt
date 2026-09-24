package com.taskchain.ui.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.platform.LocalContext
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

/** Semantic spacing tokens loaded from Android resources rather than feature literals. */
data class Spacing(
    val small: androidx.compose.ui.unit.Dp,
    val medium: androidx.compose.ui.unit.Dp,
    val large: androidx.compose.ui.unit.Dp,
)

/** Provides configured spacing to feature UI through the design-system boundary. */
object TaskChainDesignSystem {
    /** Use this function when feature UI needs resource-backed semantic spacing. */
    @Composable
    fun spacing(): Spacing = Spacing(
        small = dimensionResource(R.dimen.space_small),
        medium = dimensionResource(R.dimen.space_medium),
        large = dimensionResource(R.dimen.space_large),
    )
}

/** Use this function at the app boundary so feature UI never configures Material directly. */
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
    val colors = if (dark) {
        darkColorScheme(
            primary = colorResource(R.color.dark_primary),
            onPrimary = colorResource(R.color.dark_on_primary),
            background = colorResource(R.color.dark_background),
            onBackground = colorResource(R.color.dark_on_background),
            surface = colorResource(R.color.dark_surface),
            onSurface = colorResource(R.color.dark_on_surface),
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.light_primary),
            onPrimary = colorResource(R.color.light_on_primary),
            background = colorResource(R.color.light_background),
            onBackground = colorResource(R.color.light_on_background),
            surface = colorResource(R.color.light_surface),
            onSurface = colorResource(R.color.light_on_surface),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
