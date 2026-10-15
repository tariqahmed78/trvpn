package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GlassTokens {
    val CornerLarge = RoundedCornerShape(24.dp)
    val CornerMedium = RoundedCornerShape(18.dp)
    val CornerSmall = RoundedCornerShape(12.dp)
    val CornerPill = RoundedCornerShape(999.dp)

    val BorderWidthThin: Dp = 1.dp
    val BorderWidthAccent: Dp = 1.5.dp

    val BlurRadiusHigh: Dp = 28.dp
    val BlurRadiusMedium: Dp = 18.dp
    val BlurRadiusLight: Dp = 10.dp

    @Composable
    fun glassBorderBrush(isDark: Boolean, isFocused: Boolean = false): Brush {
        return if (isDark) {
            if (isFocused) {
                Brush.linearGradient(
                    listOf(
                        TrDarkPrimary.copy(alpha = 0.8f),
                        TrDarkSecondary.copy(alpha = 0.5f),
                        TrDarkPrimary.copy(alpha = 0.2f)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        TrDarkPrimary.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            }
        } else {
            if (isFocused) {
                Brush.linearGradient(
                    listOf(
                        TrLightPrimary.copy(alpha = 0.7f),
                        TrLightSecondary.copy(alpha = 0.4f),
                        TrLightPrimary.copy(alpha = 0.2f)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.9f),
                        TrLightPrimary.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }

    @Composable
    fun glassBackground(isDark: Boolean): Color {
        return if (isDark) TrDarkGlassBase else TrLightGlassBase
    }

    @Composable
    fun glassReflectionBrush(): Brush {
        return Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.02f),
                Color.Transparent
            )
        )
    }
}
