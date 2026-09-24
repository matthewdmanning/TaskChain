package dev.augmentedui.generator.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.augmentedui.generator.geometry.AugmentedShape
import dev.augmentedui.generator.geometry.BorderSpec
import dev.augmentedui.generator.geometry.ChamferedShape
import dev.augmentedui.generator.geometry.Cut
import dev.augmentedui.generator.geometry.CyberAugSpec
import dev.augmentedui.generator.geometry.EdgeCut
import dev.augmentedui.generator.geometry.InlaySpec
import dev.augmentedui.generator.modifier.augmentedFrame
import dev.augmentedui.generator.modifier.cyberGlow
import dev.augmentedui.generator.modifier.cyberScanlines
import dev.augmentedui.generator.theme.LocalCyberTheme

/** Use this function to render an accessible angled action with pressed-state feedback. */
@Composable
fun CyberButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val theme = LocalCyberTheme.current
    val shape = remember(theme.spacing) {
        ChamferedShape(
            topLeftCut = theme.spacing.xs,
            bottomRightCut = theme.spacing.xs,
        )
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val glow = rememberUpdatedState(if (pressed) 1f else 0.55f)
    val background = when {
        !enabled -> theme.disabledSurface
        pressed -> theme.surfacePressed
        else -> theme.surfaceRaised
    }

    Row(
        modifier = modifier
            .cyberGlow(theme.cyanGlow, theme.spacing.xs, glow, shape)
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .heightIn(min = theme.spacing.xl)
            .padding(horizontal = theme.spacing.sm, vertical = theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Use this function to group telemetry under one deterministic notched perimeter. */
@Composable
fun TelemetryCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    scanlines: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalCyberTheme.current
    val spec = remember(theme) {
        CyberAugSpec(
            topLeft = Cut.Chamfer(theme.spacing.sm),
            bottomRight = Cut.Chamfer(theme.spacing.sm),
            topEdge = EdgeCut.CenterNotch(width = theme.spacing.lg, depth = theme.spacing.xxs),
            border = BorderSpec(width = 1.dp, color = theme.cyanGlow),
            inlay = InlaySpec(inset = theme.spacing.xxs, fill = theme.surfaceRaised),
        )
    }
    val shape = remember(spec) { AugmentedShape(spec) }
    val glow = rememberUpdatedState(0.45f)
    val scanlineOffset = rememberUpdatedState(0f)
    val framed = modifier
        .cyberGlow(theme.cyanGlow, theme.spacing.xs, glow, shape)
        .clip(shape)
        .background(theme.surfaceDark)
        .augmentedFrame(spec)
    val decorated = if (scanlines) {
        framed.cyberScanlines(theme.cyanGlow.copy(alpha = 0.08f), theme.spacing.xxs, scanlineOffset)
    } else {
        framed
    }

    Column(
        modifier = decorated
            .fillMaxWidth()
            .padding(theme.spacing.sm),
    ) {
        title?.let {
            BasicText(
                text = it,
                modifier = Modifier.padding(bottom = theme.spacing.xs),
                style = theme.typography.label.copy(color = theme.textMuted),
            )
        }
        content()
    }
}

/** Use this function to display an image in a six-sided cyberpunk avatar frame. */
@Composable
fun HexAvatar(
    image: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val theme = LocalCyberTheme.current
    val spec = remember(size, theme.cyanGlow) {
        val cut = size / 4f
        CyberAugSpec(
            topLeft = Cut.ClipY(cut),
            topRight = Cut.ClipY(cut),
            bottomRight = Cut.ClipY(cut),
            bottomLeft = Cut.ClipY(cut),
            border = BorderSpec(width = 1.dp, color = theme.cyanGlow),
        )
    }
    val shape = remember(spec) { AugmentedShape(spec) }
    val glow = rememberUpdatedState(0.6f)

    Box(
        modifier = modifier
            .size(size)
            .cyberGlow(theme.cyanGlow, theme.spacing.xs, glow, shape)
            .clip(shape)
            .background(theme.surfaceRaised)
            .augmentedFrame(spec),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
