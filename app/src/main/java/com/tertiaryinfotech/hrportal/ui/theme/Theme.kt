package com.tertiaryinfotech.hrportal.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens mirrored 1:1 from the web app (hrms.tertiaryinfo.tech), which is dark-first and
 * built on flat Tailwind gray-950/900/800 surfaces with an indigo accent — see
 * `tertiary-hrms/src/app/globals.css` and `tailwind.config.ts`. No gradients: every screen is a
 * flat colored surface, exactly like the web app's pages.
 */
object Brand {
    // Surfaces (Tailwind gray-950 / 900 / 800 / 700)
    val Background = Color(0xFF030712)
    val Surface = Color(0xFF111827)
    val Border = Color(0xFF1F2937)
    val BorderLight = Color(0xFF374151)

    // Brand accent (Tailwind indigo-500, the web app's --primary)
    val Primary = Color(0xFF6366F1)

    // Text (Tailwind white / gray-400 / gray-500 / gray-600)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFF9CA3AF)
    val TextMuted = Color(0xFF6B7280)
    val TextFaint = Color(0xFF4B5563)

    // Status accents (solid — icons, values, calendar tints)
    val Amber = Color(0xFFF59E0B)
    val Green = Color(0xFF22C55E)
    val Red = Color(0xFFEF4444)
    val Blue = Color(0xFF3B82F6)
    val Purple = Color(0xFFA855F7)
    val Emerald = Color(0xFF10B981)

    const val Corner = 12 // dp — matches web's --radius: 0.75rem
    const val LogoCorner = 16 // dp — matches web's rounded-2xl logo tile
    const val ControlHeight = 56 // dp — Android touch-target minimum (web buttons are desktop-height)
}

/** Pastel badge bg/text pairs — mirrors the web's shadcn Badge tint variants exactly. */
object StatusTint {
    data class Tint(val bg: Color, val text: Color)

    val Amber = Tint(Color(0xFFFEF3C7), Color(0xFF92400E))
    val Green = Tint(Color(0xFFDCFCE7), Color(0xFF166534))
    val Red = Tint(Color(0xFFFEE2E2), Color(0xFF991B1B))
    val Blue = Tint(Color(0xFFDBEAFE), Color(0xFF1E40AF))
    val Purple = Tint(Color(0xFFF3E8FF), Color(0xFF6B21A8))
    val Emerald = Tint(Color(0xFFD1FAE5), Color(0xFF065F46))
    val Gray = Tint(Color(0xFFE5E7EB), Color(0xFF374151))
}

/** Icon-chip bg/icon pairs — mirrors the web's KPI stat-card icon tiles
 *  (`bg-amber-100`/`text-amber-600` style pairs, e.g. `dashboard/stats-cards.tsx:44-67`). */
object IconTint {
    data class Tint(val bg: Color, val icon: Color)

    val Amber = Tint(Color(0xFFFEF3C7), Color(0xFFD97706))
    val Red = Tint(Color(0xFFFEE2E2), Color(0xFFDC2626))
    val Purple = Tint(Color(0xFFF3E8FF), Color(0xFF9333EA))
    val Blue = Tint(Color(0xFFDBEAFE), Color(0xFF2563EB))
    val Green = Tint(Color(0xFFDCFCE7), Color(0xFF16A34A))
    val Emerald = Tint(Color(0xFFD1FAE5), Color(0xFF059669))
}

/** Translucent alert-banner colors — mirrors the web's dark-mode banner classes exactly
 *  (e.g. `bg-red-950/50 border-red-800 text-red-400` on the login page). */
object BannerTint {
    data class Tint(val bg: Color, val border: Color, val text: Color)

    val Error = Tint(Color(0xFF450A0A).copy(alpha = 0.5f), Color(0xFF991B1B), Color(0xFFF87171))
    val Success = Tint(Color(0xFF052E16).copy(alpha = 0.5f), Color(0xFF166534), Color(0xFF4ADE80))
}

private val PremierColorScheme = darkColorScheme(
    primary = Brand.Primary,
    onPrimary = Color.White,
    secondary = Brand.Primary,
    background = Brand.Background,
    onBackground = Color.White,
    surface = Brand.Surface,
    onSurface = Color.White,
    error = Brand.Red,
)

@Composable
fun TertiaryHRMSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PremierColorScheme,
        typography = HrmsTypography,
        content = content,
    )
}

/** Flat, full-size background matching the web app's `bg-gray-950` page background. */
@Composable
fun GradientScreen(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brand.Background)) {
            content()
        }
    }
}
