package com.example.ui.components.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassTokens

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassTokens.CornerMedium,
    backgroundColor: Color? = null,
    borderBrush: Brush? = null,
    borderWidth: Dp = GlassTokens.BorderWidthThin,
    blurRadius: Dp = GlassTokens.BlurRadiusMedium,
    isHighlighted: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val resolvedBg = backgroundColor ?: GlassTokens.glassBackground(isDark)
    val resolvedBorder = borderBrush ?: GlassTokens.glassBorderBrush(isDark, isHighlighted)

    val surfaceModifier = modifier
        .clip(shape)
        .background(resolvedBg)
        .border(
            border = BorderStroke(borderWidth, resolvedBorder),
            shape = shape
        )

    Box(modifier = surfaceModifier) {
        // Fine refractive top reflection for liquid glass sheen
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GlassTokens.glassReflectionBrush())
        )
        content()
    }
}
