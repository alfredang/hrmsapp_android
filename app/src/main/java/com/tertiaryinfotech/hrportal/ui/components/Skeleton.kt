package com.tertiaryinfotech.hrportal.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * A single pulsing placeholder block — the Compose equivalent of the web's `animate-pulse`
 * utility (Tailwind's default pulse: opacity cycling ~1 <-> 0.5 on a 2s ease-in-out loop), used
 * for skeleton loading states such as the login card's branding fetch (SCREEN_MAP.md's
 * `LoginSkeleton`). Callers size/shape it via [modifier]; the pulsing surface color is
 * [Brand.Border] to match the web's `bg-gray-800` skeleton blocks.
 */
@Composable
fun PulseBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(Brand.Corner.dp)) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Box(
        modifier = modifier.alpha(alpha).background(Brand.Border, shape),
    )
}
