package com.tertiaryinfotech.hrportal.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Premier Blue — the Tertiary HRMS brand palette.
 * A deep royal-blue identity: navy foundations rising to a vivid premier blue,
 * used consistently across the login frontend, buttons, and the brand header.
 * Mirrors `Theme/Theme.swift` from the iOS app.
 */
object Brand {
    // Core blues
    val Navy = Color(0xFF0A1F44)      // deep navy — backgrounds, gradient base
    val Premier = Color(0xFF1D4ED8)   // premier blue — primary brand / accent
    val Azure = Color(0xFF3B82F6)     // bright blue — gradient top / highlights
    val Sky = Color(0xFF94C5FD)       // soft sky — subtle accents on dark
    val Ink = Color(0xFF11161F)       // near-black text

    // Status colours (mirror the iOS StatusPill / accent tints)
    val Green = Color(0xFF34C759)
    val Yellow = Color(0xFFFFD60A)
    val Orange = Color(0xFFFF9F0A)
    val Red = Color(0xFFFF453A)
    val Pink = Color(0xFFFF6482)
    val Mint = Color(0xFF66D4CF)

    const val Corner = 16
    const val ControlHeight = 56 // >=56dp touch targets

    /** The signature Premier Blue backdrop (navy -> premier -> azure, top-leading to bottom-trailing). */
    fun backdrop(): Brush = Brush.linearGradient(
        colors = listOf(Navy, Premier, Azure),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    /** A flatter brand gradient for buttons and accents. */
    fun accent(): Brush = Brush.linearGradient(
        colors = listOf(Azure, Premier),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )
}

private val PremierColorScheme = darkColorScheme(
    primary = Brand.Premier,
    onPrimary = Color.White,
    secondary = Brand.Azure,
    background = Brand.Navy,
    onBackground = Color.White,
    surface = Brand.Navy,
    onSurface = Color.White,
    error = Brand.Red,
)

@Composable
fun TertiaryHRMSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PremierColorScheme,
        content = content,
    )
}

/** Premier Blue gradient screen background that fills the whole surface. */
@Composable
fun GradientScreen(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brand.backdrop())) {
            content()
        }
    }
}
