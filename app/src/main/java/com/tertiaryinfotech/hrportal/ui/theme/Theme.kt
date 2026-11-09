package com.tertiaryinfotech.hrportal.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * TIA HRMS "premium blue" design tokens (BRAND.md, 9 Nov 2026) on the iOS app's layout system,
 * which stays the source of truth for structure (`Theme/Theme.swift`).
 *
 * **One theme only (Alfred, 10 Nov 2026):** a single light premium-blue look, matching the web app
 * (tertiary-hrms 464200b). There is no dark mode, no dynamic colour and no theme switch. The top and
 * tab bars stay navy-950 ([Bar]); content sits on a near-white → mist-blue backdrop with white cards,
 * navy ink text and royal-600 accents. Gold-400 is the single, sparing warm accent (badges only,
 * always a fill with navy text).
 */
object Brand {
    // MARK: Brand colours (BRAND.md palette)
    /** navy-950 — bars, ink text, gold-badge text. `#0D1638`. */
    val Navy = Color(0xFF0D1638)
    /** navy-800 — headers. `#1A2F86`. */
    val Navy800 = Color(0xFF1A2F86)
    /** royal-600 — primary buttons, links, selected states. `#2F5BE6`. */
    val Premier = Color(0xFF2F5BE6)
    /** royal-500 — pressed-light, top of the button gradient. `#3D6BF0`. */
    val Azure = Color(0xFF3D6BF0)
    /** royal-300 — the accent on the navy bars. `#8FB0FF`. */
    val Sky = Color(0xFF8FB0FF)
    /** mist-100 — tinted fills and hairlines on light surfaces. `#C9D8FF`. */
    val Mist = Color(0xFFC9D8FF)
    /** gold-400 — the single warm accent (badges, unread dot). Use sparingly, as a fill. */
    val Gold = Color(0xFFF5B83D)
    /** gold-600 — gold pressed / gradient end. */
    val Gold600 = Color(0xFFD9921C)

    /** Top bar + tab bar: navy, with white content and a royal-300 accent. */
    val Bar = Navy
    val OnBar = Color.White
    val BarAccent = Sky

    /** Brand fill (royal-600) for buttons and selected chips. */
    val Primary = Premier

    // MARK: Surfaces
    /** Opaque app background behind the backdrop (Material background/surface). */
    val Background = Color(0xFFEFF4FF)
    /** Card fill. */
    val Surface = Color.White
    /** Hero / highlighted card fill (very light blue). */
    val SurfaceStrong = Color(0xFFEAF1FF)
    val Border = Color(0xFFD5E1F7)
    val BorderLight = Color(0xFFC2D3F0)
    val Field = Color(0xFFF3F6FD)
    val FieldBorder = Color(0xFFD5E1F7)
    /** Opaque container for dialogs, menus, sheets and pickers. */
    val Dialog = Color.White

    // MARK: Text
    val TextPrimary = Navy
    val TextSecondary = Color(0xFF3A4A7A)
    val TextMuted = Color(0xFF64748B)
    val TextFaint = Color(0xFF94A3B8)

    // MARK: Semantic colours (AA on white)
    val Green = Color(0xFF15803D)
    val Mint = Color(0xFF0F766E)
    val Red = Color(0xFFB91C1C)
    val Amber = Color(0xFFB45309)
    val Purple = Color(0xFF7E22CE)
    val Orange = Color(0xFFC2410C)
    val Pink = Color(0xFFBE123C)
    val Teal = Color(0xFF0F766E)
    /** Accent for text, icons and selected states (royal-600). [Primary] is the fill twin. */
    val Accent = Premier
    // Kept for call sites that referenced these previously.
    val Blue = Premier
    val Emerald = Green

    /** Backdrop stops for [GradientScreen]: near-white → mist-blue. */
    val GradientStops: List<Color> = listOf(Color(0xFFF7F9FF), Color(0xFFEEF2FF), Color(0xFFE4EBFF))

    /** iOS `Theme.accent` — the gradient on primary buttons, avatars and selected chips. */
    val AccentGradient: Brush = Brush.linearGradient(listOf(Azure, Premier))

    const val Corner = 16 // dp — iOS Theme.corner
    const val CardCorner = 18 // dp — iOS Card
    const val SmallCorner = 12 // dp — iOS banners, form rows and secondary buttons
    const val LogoCorner = 24 // dp — iOS BrandHeader
    const val ControlHeight = 56 // dp — iOS Theme.controlHeight
}

/** Pastel badge bg/text pairs for status pills. */
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

/** Icon-chip bg/icon pairs for KPI stat tiles (Home): a light-blue chip behind a coloured glyph. */
object IconTint {
    data class Tint(val bg: Color, val icon: Color)

    private val chip = Color(0xFFEAF1FF)
    val Amber = Tint(chip, Brand.Amber)
    val Red = Tint(chip, Brand.Red)
    val Purple = Tint(chip, Brand.Purple)
    val Blue = Tint(chip, Brand.Premier)
    val Green = Tint(chip, Brand.Green)
    val Emerald = Tint(chip, Brand.Green)
}

/** Alert-banner colours. */
object BannerTint {
    data class Tint(val bg: Color, val border: Color, val text: Color)

    val Error = Tint(Color(0xFFFEE2E2), Color(0xFFFCA5A5), Color(0xFF991B1B))
    val Success = Tint(Color(0xFFDCFCE7), Color(0xFF86EFAC), Color(0xFF166534))
}

@Composable
fun TertiaryHRMSTheme(content: @Composable () -> Unit) {
    // Light only: no dark scheme, no dynamic colour.
    val colorScheme = lightColorScheme(
        primary = Brand.Accent,
        onPrimary = Color.White,
        primaryContainer = Brand.SurfaceStrong,
        onPrimaryContainer = Brand.TextPrimary,
        inversePrimary = Brand.Sky,

        secondary = Brand.Accent,
        onSecondary = Color.White,
        secondaryContainer = Brand.Border,
        onSecondaryContainer = Brand.TextPrimary,
        // The brand's single warm accent replaces Material's default purple tertiary.
        tertiary = Brand.Gold,
        onTertiary = Brand.Navy,

        background = Brand.Background,
        onBackground = Brand.TextPrimary,
        surface = Brand.Background,
        onSurface = Brand.TextPrimary,
        surfaceVariant = Brand.Border,
        onSurfaceVariant = Brand.TextSecondary,
        surfaceTint = Brand.Primary,

        error = Brand.Red,
        onError = Color.White,

        outline = Brand.BorderLight,
        outlineVariant = Brand.Border,

        // Opaque: dialogs, menus, sheets and date pickers read these.
        surfaceContainer = Brand.Dialog,
        surfaceContainerHigh = Brand.Dialog,
        surfaceContainerHighest = Brand.Dialog,
        surfaceContainerLow = Brand.Dialog,
        surfaceBright = Brand.Dialog,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HrmsTypography,
        shapes = HrmsShapes,
        content = content,
    )
}

/** The light premium-blue backdrop (near-white → mist-blue diagonal). Every screen renders inside this. */
@Composable
fun GradientScreen(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Brand.Background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = Brand.GradientStops,
                        start = Offset(0f, 0f),
                        end = Offset.Infinite,
                    )
                )
        ) {
            content()
        }
    }
}
