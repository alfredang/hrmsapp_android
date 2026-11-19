package com.tertiaryinfotech.hrportal.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
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
 * **Two palettes (Alfred, 19 Nov 2026; replaces the one-theme rule of 10 Nov):** Light is the
 * premium-blue look (navy bars, near-white to mist-blue backdrop, white cards, navy ink, royal-600
 * accents). Dark matches the web app's dark (`#030712` page, `#111827` cards, near-white text,
 * royal-300 accent text). The person picks Light, Dark or System in My Profile ([Appearance]).
 * No dynamic colour. Every theme-dependent token below is a getter over [Appearance.isDark]
 * (Compose snapshot state), so call sites keep reading `Brand.X` and repaint when the choice changes.
 * Never copy one into a top-level `val` or a `remember` without the theme as a key.
 * Gold-400 is the single, sparing warm accent (badges only, always a fill with navy text).
 */
object Brand {
    private val dark: Boolean get() = Appearance.isDark
    private fun pick(light: Long, dark: Long): Color = Color(if (this.dark) dark else light)

    // MARK: Brand colours (BRAND.md palette) - the same in both themes
    /** navy-950 - light-theme bars and ink, gold-badge text. `#0D1638`. */
    val Navy = Color(0xFF0D1638)
    /** navy-800 - headers. `#1A2F86`. */
    val Navy800 = Color(0xFF1A2F86)
    /** royal-600 - primary FILL (buttons, selected chips, today). White text on it in both themes.
     *  For text or icons use [Accent], which lightens in dark. `#2F5BE6`. */
    val Premier = Color(0xFF2F5BE6)
    /** royal-500 - pressed-light, top of the button gradient. `#3D6BF0`. */
    val Azure = Color(0xFF3D6BF0)
    /** royal-300 - the accent on the bars, and accent text in dark. `#8FB0FF`. */
    val Sky = Color(0xFF8FB0FF)
    /** mist-100 - tinted fills and hairlines on light surfaces. `#C9D8FF`. */
    val Mist = Color(0xFFC9D8FF)
    /** gold-400 - the single warm accent (badges, unread dot). Use sparingly, as a fill. */
    val Gold = Color(0xFFF5B83D)
    /** gold-600 - gold pressed / gradient end. */
    val Gold600 = Color(0xFFD9921C)

    /** Top bar + tab bar: navy in light, the card grey in dark; white content, royal-300 accent. */
    val Bar: Color get() = pick(0xFF0D1638, 0xFF111827)
    val OnBar = Color.White
    val BarAccent = Sky

    /** Brand fill (royal-600) for buttons and selected chips. */
    val Primary = Premier

    // MARK: Surfaces
    /** Opaque app background behind the backdrop (Material background/surface). */
    val Background: Color get() = pick(0xFFEFF4FF, 0xFF030712)
    /** Card fill. */
    val Surface: Color get() = pick(0xFFFFFFFF, 0xFF111827)
    /** Hero / highlighted card fill (very light blue; a blue-grey lift in dark). */
    val SurfaceStrong: Color get() = pick(0xFFEAF1FF, 0xFF1C2740)
    /** Hairlines: white at about 10 percent in dark. */
    val Border: Color get() = pick(0xFFD5E1F7, 0x1AFFFFFF)
    val BorderLight: Color get() = pick(0xFFC2D3F0, 0x29FFFFFF)
    val Field: Color get() = pick(0xFFF3F6FD, 0xFF0B1220)
    val FieldBorder: Color get() = pick(0xFFD5E1F7, 0x29FFFFFF)
    /** Opaque container for dialogs, menus, sheets and pickers. */
    val Dialog: Color get() = pick(0xFFFFFFFF, 0xFF1F2937)
    /** Light grey fill for a public-holiday calendar cell. */
    val HolidayCell: Color get() = pick(0xFFE9ECF2, 0xFF1F2937)

    // MARK: Text
    val TextPrimary: Color get() = pick(0xFF0D1638, 0xFFF9FAFB)
    val TextSecondary: Color get() = pick(0xFF3A4A7A, 0xFF9CA3AF)
    val TextMuted: Color get() = pick(0xFF64748B, 0xFF8B95A5)
    val TextFaint: Color get() = pick(0xFF94A3B8, 0xFF6B7280)

    // MARK: Semantic colours (AA on the card in each theme)
    val Green: Color get() = pick(0xFF15803D, 0xFF4ADE80)
    val Mint: Color get() = pick(0xFF0F766E, 0xFF2DD4BF)
    val Red: Color get() = pick(0xFFB91C1C, 0xFFF87171)
    val Amber: Color get() = pick(0xFFB45309, 0xFFFBBF24)
    val Purple: Color get() = pick(0xFF7E22CE, 0xFFC084FC)
    val Orange: Color get() = pick(0xFFC2410C, 0xFFFB923C)
    val Pink: Color get() = pick(0xFFBE123C, 0xFFFB7185)
    val Teal: Color get() = Mint
    /** Accent for text, icons and selected states: royal-600 in light, royal-300 in dark.
     *  [Primary] is the fill twin. */
    val Accent: Color get() = pick(0xFF2F5BE6, 0xFF8FB0FF)
    // Kept for call sites that referenced these previously (text / icon / dot uses).
    val Blue: Color get() = Accent
    val Emerald: Color get() = Green

    /** Backdrop stops for [GradientScreen]: near-white to mist-blue; near-black in dark. */
    val GradientStops: List<Color>
        get() = if (dark) listOf(Color(0xFF030712), Color(0xFF050A18), Color(0xFF070D1F))
        else listOf(Color(0xFFF7F9FF), Color(0xFFEEF2FF), Color(0xFFE4EBFF))

    /** iOS `Theme.accent` - the gradient on primary buttons, avatars and selected chips. */
    val AccentGradient: Brush = Brush.linearGradient(listOf(Azure, Premier))

    const val Corner = 16 // dp - iOS Theme.corner
    const val CardCorner = 18 // dp - iOS Card
    const val SmallCorner = 12 // dp - iOS banners, form rows and secondary buttons
    const val LogoCorner = 24 // dp - iOS BrandHeader
    const val ControlHeight = 56 // dp - iOS Theme.controlHeight
}

/** Badge bg/text pairs for status pills: pastel in light, a translucent tint with light text in dark. */
object StatusTint {
    data class Tint(val bg: Color, val text: Color)

    private fun tint(lightBg: Long, lightText: Long, darkBg: Long, darkText: Long): Tint =
        if (Appearance.isDark) Tint(Color(darkBg), Color(darkText)) else Tint(Color(lightBg), Color(lightText))

    val Amber: Tint get() = tint(0xFFFEF3C7, 0xFF92400E, 0x33F59E0B, 0xFFFCD34D)
    val Green: Tint get() = tint(0xFFDCFCE7, 0xFF166534, 0x3322C55E, 0xFF86EFAC)
    val Red: Tint get() = tint(0xFFFEE2E2, 0xFF991B1B, 0x33EF4444, 0xFFFCA5A5)
    val Blue: Tint get() = tint(0xFFDBEAFE, 0xFF1E40AF, 0x333B82F6, 0xFF93C5FD)
    val Purple: Tint get() = tint(0xFFF3E8FF, 0xFF6B21A8, 0x33A855F7, 0xFFD8B4FE)
    val Emerald: Tint get() = tint(0xFFD1FAE5, 0xFF065F46, 0x3310B981, 0xFF6EE7B7)
    val Gray: Tint get() = tint(0xFFE5E7EB, 0xFF374151, 0x339CA3AF, 0xFFD1D5DB)
}

/** Icon-chip bg/icon pairs for KPI stat tiles (Home): a soft blue chip behind a coloured glyph. */
object IconTint {
    data class Tint(val bg: Color, val icon: Color)

    private val chip: Color get() = if (Appearance.isDark) Color(0x1F8FB0FF) else Color(0xFFEAF1FF)
    val Amber: Tint get() = Tint(chip, Brand.Amber)
    val Red: Tint get() = Tint(chip, Brand.Red)
    val Purple: Tint get() = Tint(chip, Brand.Purple)
    val Blue: Tint get() = Tint(chip, Brand.Accent)
    val Green: Tint get() = Tint(chip, Brand.Green)
    val Emerald: Tint get() = Tint(chip, Brand.Green)
}

/** Alert-banner colours. */
object BannerTint {
    data class Tint(val bg: Color, val border: Color, val text: Color)

    val Error: Tint
        get() = if (Appearance.isDark) Tint(Color(0x33EF4444), Color(0x66F87171), Color(0xFFFCA5A5))
        else Tint(Color(0xFFFEE2E2), Color(0xFFFCA5A5), Color(0xFF991B1B))
    val Success: Tint
        get() = if (Appearance.isDark) Tint(Color(0x3322C55E), Color(0x664ADE80), Color(0xFF86EFAC))
        else Tint(Color(0xFFDCFCE7), Color(0xFF86EFAC), Color(0xFF166534))
}

@Composable
fun TertiaryHRMSTheme(content: @Composable () -> Unit) {
    // Light or dark from Appearance (My Profile); no dynamic colour. The Brand tokens already
    // follow the theme, so one mapping serves both schemes.
    val dark = Appearance.isDark
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val colorScheme = base.copy(
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
        surfaceDim = Brand.Background,
        surfaceContainerLowest = Brand.Dialog,
        inverseSurface = Brand.TextPrimary,
        inverseOnSurface = Brand.Background,
        scrim = Color.Black,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HrmsTypography,
        shapes = HrmsShapes,
        content = content,
    )
}

/** The page backdrop (near-white to mist-blue in light, near-black in dark). Every screen renders inside this. */
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
