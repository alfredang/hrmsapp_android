package com.tertiaryinfotech.hrportal.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens mirrored 1:1 from the web app (hrms.tertiaryinfo.tech): dark by default, built on
 * flat Tailwind gray-950/900/800 surfaces with an indigo accent, plus the web's own light-mode
 * remap (`globals.css`'s `html.light` overrides), switched at runtime via [Brand.applyTheme]. No
 * gradients: every screen is a flat colored surface in both themes.
 *
 * Every surface/text/status token below is a `mutableStateOf`-backed `var` so existing call sites
 * (`Brand.Background`, `Brand.TextPrimary`, etc., used directly — not through
 * `MaterialTheme.colorScheme` — across every screen) stay untouched syntactically, but now read
 * live theme state and recompose automatically when the user flips Dark/Light on the Profile
 * screen's Theme card.
 */
object Brand {
    /** Shared shape for [DarkPalette]/[LightPalette] so [applyTheme] can hold either as one
     *  reference — two unrelated `object`s have no common members otherwise. */
    private interface PaletteTokens {
        val Background: Color
        val Surface: Color
        val Border: Color
        val BorderLight: Color
        val TextPrimary: Color
        val TextSecondary: Color
        val TextMuted: Color
        val TextFaint: Color
        val Amber: Color
        val Green: Color
        val Red: Color
        val Blue: Color
        val Purple: Color
        val Emerald: Color
    }

    // Dark palette — Tailwind gray-950/900/800/700 (default, matches the web's `.dark` variables)
    private object DarkPalette : PaletteTokens {
        override val Background = Color(0xFF030712)
        override val Surface = Color(0xFF111827)
        override val Border = Color(0xFF1F2937)
        override val BorderLight = Color(0xFF374151)
        override val TextPrimary = Color.White
        override val TextSecondary = Color(0xFF9CA3AF)
        override val TextMuted = Color(0xFF6B7280)
        override val TextFaint = Color(0xFF4B5563)
        override val Amber = Color(0xFFF59E0B)
        override val Green = Color(0xFF22C55E)
        override val Red = Color(0xFFEF4444)
        override val Blue = Color(0xFF3B82F6)
        override val Purple = Color(0xFFA855F7)
        override val Emerald = Color(0xFF10B981)
    }

    // Light palette — exact hex values lifted from the web's `html.light` overrides in
    // `tertiary-hrms/src/app/globals.css` (bg-gray-950→#fff, text-white→#0f172a, etc.)
    private object LightPalette : PaletteTokens {
        override val Background = Color(0xFFFFFFFF)
        override val Surface = Color(0xFFF8FAFC)
        override val Border = Color(0xFFE2E8F0)
        override val BorderLight = Color(0xFFCBD5E1)
        override val TextPrimary = Color(0xFF0F172A)
        override val TextSecondary = Color(0xFF475569)
        override val TextMuted = Color(0xFF64748B)
        override val TextFaint = Color(0xFF94A3B8)
        override val Amber = Color(0xFFB45309)
        override val Green = Color(0xFF15803D)
        override val Red = Color(0xFFB91C1C)
        override val Blue = Color(0xFF1D4ED8)
        override val Purple = Color(0xFF7E22CE)
        override val Emerald = Color(0xFF047857)
    }

    var isDark: Boolean by mutableStateOf(true)
        private set

    // Surfaces
    var Background: Color by mutableStateOf(DarkPalette.Background); private set
    var Surface: Color by mutableStateOf(DarkPalette.Surface); private set
    var Border: Color by mutableStateOf(DarkPalette.Border); private set
    var BorderLight: Color by mutableStateOf(DarkPalette.BorderLight); private set

    // Brand accent (Tailwind indigo-500, the web app's --primary) — identical in both themes
    val Primary = Color(0xFF6366F1)

    // Text
    var TextPrimary: Color by mutableStateOf(DarkPalette.TextPrimary); private set
    var TextSecondary: Color by mutableStateOf(DarkPalette.TextSecondary); private set
    var TextMuted: Color by mutableStateOf(DarkPalette.TextMuted); private set
    var TextFaint: Color by mutableStateOf(DarkPalette.TextFaint); private set

    // Status accents (solid — icons, values, calendar tints)
    var Amber: Color by mutableStateOf(DarkPalette.Amber); private set
    var Green: Color by mutableStateOf(DarkPalette.Green); private set
    var Red: Color by mutableStateOf(DarkPalette.Red); private set
    var Blue: Color by mutableStateOf(DarkPalette.Blue); private set
    var Purple: Color by mutableStateOf(DarkPalette.Purple); private set
    var Emerald: Color by mutableStateOf(DarkPalette.Emerald); private set

    const val Corner = 12 // dp — matches web's --radius: 0.75rem
    const val LogoCorner = 16 // dp — matches web's rounded-2xl logo tile
    const val ControlHeight = 56 // dp — Android touch-target minimum (web buttons are desktop-height)

    /** Switches every reactive token between the dark (default) and light palette. Called once at
     *  startup from the persisted [ThemePrefs] choice, and again whenever the user flips the
     *  Theme card on the Profile screen. */
    fun applyTheme(dark: Boolean) {
        isDark = dark
        val p: PaletteTokens = if (dark) DarkPalette else LightPalette
        Background = p.Background
        Surface = p.Surface
        Border = p.Border
        BorderLight = p.BorderLight
        TextPrimary = p.TextPrimary
        TextSecondary = p.TextSecondary
        TextMuted = p.TextMuted
        TextFaint = p.TextFaint
        Amber = p.Amber
        Green = p.Green
        Red = p.Red
        Blue = p.Blue
        Purple = p.Purple
        Emerald = p.Emerald
        BannerTint.applyTheme(dark)
    }
}

/** Pastel badge bg/text pairs — mirrors the web's shadcn Badge tint variants exactly. These are
 *  the same pastel-on-dark-text pairs in both themes (the web app doesn't remap them either — a
 *  status badge looks the same regardless of page theme). */
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
 *  (`bg-amber-100`/`text-amber-600` style pairs, e.g. `dashboard/stats-cards.tsx:44-67`). Same
 *  pastel pairs in both themes, same reasoning as [StatusTint]. */
object IconTint {
    data class Tint(val bg: Color, val icon: Color)

    val Amber = Tint(Color(0xFFFEF3C7), Color(0xFFD97706))
    val Red = Tint(Color(0xFFFEE2E2), Color(0xFFDC2626))
    val Purple = Tint(Color(0xFFF3E8FF), Color(0xFF9333EA))
    val Blue = Tint(Color(0xFFDBEAFE), Color(0xFF2563EB))
    val Green = Tint(Color(0xFFDCFCE7), Color(0xFF16A34A))
    val Emerald = Tint(Color(0xFFD1FAE5), Color(0xFF059669))
}

/** Translucent alert-banner colors — mirrors the web's dark-mode banner classes
 *  (`bg-red-950/50 border-red-800 text-red-400`) and their `html.light` overrides
 *  (`bg-red-950/30 → #fee2e2`, `text-red-300 → #991b1b`) exactly. Reactive like [Brand]. */
object BannerTint {
    data class Tint(val bg: Color, val border: Color, val text: Color)

    private object DarkPalette {
        val Error = Tint(Color(0xFF450A0A).copy(alpha = 0.5f), Color(0xFF991B1B), Color(0xFFF87171))
        val Success = Tint(Color(0xFF052E16).copy(alpha = 0.5f), Color(0xFF166534), Color(0xFF4ADE80))
    }

    private object LightPalette {
        val Error = Tint(Color(0xFFFEE2E2), Color(0xFFFCA5A5), Color(0xFF991B1B))
        val Success = Tint(Color(0xFFDCFCE7), Color(0xFF86EFAC), Color(0xFF166534))
    }

    var Error: Tint by mutableStateOf(DarkPalette.Error); private set
    var Success: Tint by mutableStateOf(DarkPalette.Success); private set

    internal fun applyTheme(dark: Boolean) {
        Error = if (dark) DarkPalette.Error else LightPalette.Error
        Success = if (dark) DarkPalette.Success else LightPalette.Success
    }
}

@Composable
fun TertiaryHRMSTheme(content: @Composable () -> Unit) {
    // Every field below reads a reactive Brand.* token, so this recomposes automatically whenever
    // Brand.applyTheme() runs — the dark/light builder choice itself doesn't matter since every
    // role used by this app is explicitly overridden (see comment history), so darkColorScheme()
    // is kept as the single builder for both themes to avoid duplicating ~30 named args.
    val colorScheme = darkColorScheme(
        primary = Brand.Primary,
        onPrimary = Color.White,
        primaryContainer = Brand.Surface,
        onPrimaryContainer = Brand.Primary,
        inversePrimary = Brand.Primary,

        // Single-accent brand (DESIGN_SYSTEM.md has one accent, indigo) — secondary/tertiary alias
        // back to it/the status palette rather than falling back to Material's default teal/purple.
        secondary = Brand.Primary,
        onSecondary = Color.White,
        secondaryContainer = Brand.Border,
        onSecondaryContainer = Brand.TextPrimary,
        tertiary = Brand.Purple,
        onTertiary = Color.White,
        tertiaryContainer = Brand.Border,
        onTertiaryContainer = Brand.TextPrimary,

        background = Brand.Background,
        onBackground = Brand.TextPrimary,
        surface = Brand.Surface,
        onSurface = Brand.TextPrimary,
        surfaceVariant = Brand.Border,
        onSurfaceVariant = Brand.TextSecondary,
        surfaceTint = Brand.Primary,
        inverseSurface = Brand.TextPrimary,
        inverseOnSurface = Brand.Background,

        error = Brand.Red,
        onError = Color.White,
        errorContainer = if (Brand.isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2),
        onErrorContainer = if (Brand.isDark) Color(0xFFF87171) else Color(0xFF991B1B),

        outline = Brand.BorderLight,
        outlineVariant = Brand.Border,
        scrim = Color.Black,

        // Tonal-elevation surface steps — flattened to our own surface/border scale rather than
        // Material's tonal-overlay ramp, since the app is intentionally flat (no gradients/elevation
        // tinting; CLAUDE.md).
        surfaceBright = Brand.BorderLight,
        surfaceDim = Brand.Background,
        surfaceContainerLowest = Brand.Background,
        surfaceContainerLow = Brand.Background,
        surfaceContainer = Brand.Surface,
        surfaceContainerHigh = Brand.Border,
        surfaceContainerHighest = Brand.BorderLight,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HrmsTypography,
        shapes = HrmsShapes,
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
