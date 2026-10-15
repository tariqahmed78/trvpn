package com.example.ui.components.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassTokens.CornerLarge,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color? = null,
    borderBrush: Brush? = null,
    isHighlighted: Boolean = false,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        modifier
    }

    GlassSurface(
        modifier = clickableModifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderBrush = borderBrush,
        isHighlighted = isHighlighted
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
