package com.example.ui.components.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassTokens

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GlassTokens.CornerMedium,
    backgroundColor: Color? = null,
    borderBrush: Brush? = null,
    isPrimary: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val resolvedBg = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        isPrimary -> if (isDark) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        }
        backgroundColor != null -> backgroundColor
        else -> GlassTokens.glassBackground(isDark)
    }

    val resolvedBorder = when {
        !enabled -> null
        isPrimary -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            )
        )
        borderBrush != null -> borderBrush
        else -> GlassTokens.glassBorderBrush(isDark)
    }

    val interactionSource = remember { MutableInteractionSource() }

    GlassSurface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        shape = shape,
        backgroundColor = resolvedBg,
        borderBrush = resolvedBorder,
        borderWidth = if (isPrimary) GlassTokens.BorderWidthAccent else GlassTokens.BorderWidthThin
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
