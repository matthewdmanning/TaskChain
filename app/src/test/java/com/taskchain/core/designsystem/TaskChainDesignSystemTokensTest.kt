package com.taskchain.core.designsystem

import androidx.compose.ui.unit.dp
import com.example.cyberpunkandroid.config.CyberPrimitives
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies that feature-facing dimension tokens keep the exact values features used before the token migration. */
class TaskChainDesignSystemTokensTest {
    /** The spacing scale reads the library grid one-to-one. */
    @Test
    fun spaceScaleMatchesLibraryGrid() {
        assertEquals(CyberPrimitives.Spacing.dp4, TaskChainDesignSystem.Space.extraSmall)
        assertEquals(CyberPrimitives.Spacing.dp8, TaskChainDesignSystem.Space.small)
        assertEquals(CyberPrimitives.Spacing.dp12, TaskChainDesignSystem.Space.smallMedium)
        assertEquals(CyberPrimitives.Spacing.dp16, TaskChainDesignSystem.Space.medium)
        assertEquals(CyberPrimitives.Spacing.dp24, TaskChainDesignSystem.Space.large)
        assertEquals(CyberPrimitives.Spacing.dp32, TaskChainDesignSystem.Space.extraLarge)
    }

    /** Component sizes keep the library sizes and the former runner literal. */
    @Test
    fun sizesMatchFormerValues() {
        assertEquals(CyberPrimitives.IconSizes.dp48, TaskChainDesignSystem.Sizes.touchTarget)
        assertEquals(CyberPrimitives.IconSizes.dp64, TaskChainDesignSystem.Sizes.prominentTouchTarget)
        assertEquals(CyberPrimitives.IconSizes.dp48, TaskChainDesignSystem.Sizes.iconLarge)
        assertEquals(CyberPrimitives.IconSizes.dp24, TaskChainDesignSystem.Sizes.iconMedium)
        assertEquals(CyberPrimitives.IconSizes.dp16, TaskChainDesignSystem.Sizes.iconSmall)
        assertEquals(CyberPrimitives.Spacing.dp16, TaskChainDesignSystem.Sizes.inlineIcon)
        assertEquals(112.dp, TaskChainDesignSystem.Sizes.actionMinWidth)
        assertEquals(CyberPrimitives.IconSizes.dp48 + CyberPrimitives.Spacing.dp32, TaskChainDesignSystem.Sizes.emptySectionHeight)
        assertEquals(CyberPrimitives.Spacing.dp8, TaskChainDesignSystem.Sizes.cardGlowRadius)
    }

    /** Strokes keep the library border widths and the former literal outline widths. */
    @Test
    fun strokesMatchFormerValues() {
        assertEquals(CyberPrimitives.BorderWidths.dp1, TaskChainDesignSystem.Strokes.hairline)
        assertEquals(CyberPrimitives.BorderWidths.dp2, TaskChainDesignSystem.Strokes.emphasis)
        assertEquals(2.dp, TaskChainDesignSystem.Strokes.primaryActionBorder)
        assertEquals(3.dp, TaskChainDesignSystem.Strokes.alertBorder)
    }

    /** Countdown dial and subtask rim geometry keeps its former values. */
    @Test
    fun dialGeometryMatchesFormerValues() {
        assertEquals(CyberPrimitives.IconSizes.dp48, TaskChainDesignSystem.Dial.minDiameter)
        assertEquals(CyberPrimitives.Spacing.dp32, TaskChainDesignSystem.Dial.screenInset)
        assertEquals(CyberPrimitives.BorderWidths.dp2 * 3, TaskChainDesignSystem.Dial.rimThickness)
        assertEquals(CyberPrimitives.Spacing.dp12, TaskChainDesignSystem.Dial.tickLength)
        assertEquals(CyberPrimitives.Spacing.dp24, TaskChainDesignSystem.Dial.majorTickLength)
        assertEquals(2.dp, TaskChainDesignSystem.Dial.sweepStroke)
        assertEquals(CyberPrimitives.Spacing.dp8, TaskChainDesignSystem.Dial.timerGlowRadius)
        assertEquals(CyberPrimitives.Spacing.dp16, TaskChainDesignSystem.Dial.urgentTimerGlowRadius)
        assertEquals(CyberPrimitives.Spacing.dp8, TaskChainDesignSystem.Dial.subtaskRimSlot)
        assertEquals(3.dp, TaskChainDesignSystem.Dial.subtaskRimMaxStroke)
    }

    /** The RunnerNextUpPreview gap moved from a literal 8dp onto the grid token. */
    @Test
    fun nextUpGapKeepsFormerLiteral() {
        assertEquals(8.dp, TaskChainDesignSystem.Space.small)
    }
}
