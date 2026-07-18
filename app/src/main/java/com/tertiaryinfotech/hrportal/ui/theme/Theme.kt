package com.tertiaryinfotech.hrportal.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Premier Blue design tokens — pixel-matched to the native **iOS** app (`Theme/Theme.swift`), which
 * is the visual source of truth for this app. A deep royal-blue identity: navy foundations rising
 * to a vivid premier blue then bright azure. The signature backdrop is the navy → premier → azure
 * diagonal gradient ([GradientScreen]); cards are translucent frosted panels on top of it.
 *
 * Unlike iOS (dark-only today), this Android build keeps a **light / dark toggle** — both themed in
 * Premier Blue. Dark is the iOS look (navy→azure gradient, frosted-white cards, white text). Light
 * is a Premier-Blue light remap (white / very-light-blue surfaces, premier-blue accents, dark ink
 * text) so the brand identity is preserved in both modes. iOS will gain the same toggle later.
 *
 * Every surface/text token below is a `mutableStateOf`-backed `var` so existing call sites
 * (`Brand.Background`, `Brand.TextPrimary`, `Brand.Surface`, …) stay untouched syntactically but now
 * read live theme state and recompose automatically when the user flips Dark/Light on Profile.
 */
object Brand {
    // MARK: Fixed brand blues (exact hex from the iOS Theme.swift) — same in both themes.
    /** Deep navy — dark gradient base / opaque bars. `#0A1F44`. */
    val Navy = Color(0xFF0A1F44)
    /** Premier blue — primary brand / accent. `#1D4ED8`. */
    val Premier = Color(0xFF1D4ED8)
    /** Bright azure — gradient top / highlights. `#3B82F6`. */
    val Azure = Color(0xFF3B82F6)
    /** Soft sky — subtle accents. `#94C5FD`. */
    val Sky = Color(0xFF94C5FD)

    /** Brand accent (premier blue) — button + tab tint. Identical in both themes. */
    val Primary = Premier

    private interface PaletteTokens {
        val Background: Color        // opaque bar colour (top bar / bottom nav)
        val Surface: Color           // card fill
        val SurfaceStrong: Color     // brighter card fill (hero / balances)
        val Border: Color
        val BorderLight: Color
        val Field: Color
        val FieldBorder: Color
        val TextPrimary: Color
        val TextSecondary: Color
        val TextMuted: Color
        val TextFaint: Color
        val Green: Color             // money / positive
        val Mint: Color
        val Red: Color
        val Amber: Color
        val Purple: Color
        val gradient: List<Color>    // GradientScreen stops
    }

    /** Dark = the iOS Premier Blue look: navy→premier→azure gradient, frosted-white cards, white text. */
    private object DarkPalette : PaletteTokens {
        override val Background = Navy
        override val Surface = Color.White.copy(alpha = 0.08f)
        override val SurfaceStrong = Color.White.copy(alpha = 0.12f)
        override val Border = Color.White.copy(alpha = 0.18f)
        override val BorderLight = Color.White.copy(alpha = 0.22f)
        override val Field = Color.White.copy(alpha = 0.12f)
        override val FieldBorder = Color.White.copy(alpha = 0.22f)
        override val TextPrimary = Color.White
        override val TextSecondary = Color.White.copy(alpha = 0.72f)
        override val TextMuted = Color.White.copy(alpha = 0.55f)
        override val TextFaint = Color.White.copy(alpha = 0.40f)
        override val Green = Color(0xFF34D399)
        override val Mint = Color(0xFF6EE7B7)
        override val Red = Color(0xFFF87171)
        override val Amber = Color(0xFFFBBF24)
        override val Purple = Color(0xFFC4B5FD)
        override val gradient = listOf(Navy, Premier, Azure)
    }

    /** Light = Premier-Blue light remap: near-white / very-light-blue surfaces, blue accents, ink text. */
    private object LightPalette : PaletteTokens {
        override val Background = Color(0xFFEFF4FF)                 // very light blue app bar
        override val Surface = Color.White                          // solid white cards
        override val SurfaceStrong = Color(0xFFEAF1FF)              // light-blue hero card
        override val Border = Color(0xFFD5E1F7)
        override val BorderLight = Color(0xFFC2D3F0)
        override val Field = Color(0xFFF3F6FD)
        override val FieldBorder = Color(0xFFD5E1F7)
        override val TextPrimary = Color(0xFF0A1F44)               // navy ink
        override val TextSecondary = Color(0xFF3B5680)
        override val TextMuted = Color(0xFF64748B)
        override val TextFaint = Color(0xFF94A3B8)
        override val Green = Color(0xFF15803D)
        override val Mint = Color(0xFF0F766E)
        override val Red = Color(0xFFB91C1C)
        override val Amber = Color(0xFFB45309)
        override val Purple = Color(0xFF7E22CE)
        override val gradient = listOf(Color(0xFFF7FAFF), Color(0xFFEFF4FF), Color(0xFFE7EFFF))
    }

    var isDark: Boolean by mutableStateOf(true)
        private set

    var Background: Color by mutableStateOf(DarkPalette.Background); private set
    var Surface: Color by mutableStateOf(DarkPalette.Surface); private set
    var SurfaceStrong: Color by mutableStateOf(DarkPalette.SurfaceStrong); private set
    var Border: Color by mutableStateOf(DarkPalette.Border); private set
    var BorderLight: Color by mutableStateOf(DarkPalette.BorderLight); private set
    var Field: Color by mutableStateOf(DarkPalette.Field); private set
    var FieldBorder: Color by mutableStateOf(DarkPalette.FieldBorder); private set

    var TextPrimary: Color by mutableStateOf(DarkPalette.TextPrimary); private set
    var TextSecondary: Color by mutableStateOf(DarkPalette.TextSecondary); private set
    var TextMuted: Color by mutableStateOf(DarkPalette.TextMuted); private set
    var TextFaint: Color by mutableStateOf(DarkPalette.TextFaint); private set

    var Green: Color by mutableStateOf(DarkPalette.Green); private set
    var Mint: Color by mutableStateOf(DarkPalette.Mint); private set
    var Red: Color by mutableStateOf(DarkPalette.Red); private set
    var Amber: Color by mutableStateOf(DarkPalette.Amber); private set
    var Purple: Color by mutableStateOf(DarkPalette.Purple); private set
    // Kept for call sites that referenced these previously.
    val Blue = Sky
    var Emerald: Color by mutableStateOf(DarkPalette.Green); private set

    var GradientStops: List<Color> by mutableStateOf(DarkPalette.gradient); private set

    const val Corner = 16 // dp — iOS Theme.corner
    const val LogoCorner = 16 // dp
    const val ControlHeight = 56 // dp — iOS Theme.controlHeight

    /** Switches every reactive token between the dark (default) and light Premier-Blue palette.
     *  Called once at startup from persisted [ThemePrefs], and whenever the user flips the Theme
     *  card on Profile. */
    fun applyTheme(dark: Boolean) {
        isDark = dark
        val p: PaletteTokens = if (dark) DarkPalette else LightPalette
        Background = p.Background
        Surface = p.Surface
        SurfaceStrong = p.SurfaceStrong
        Border = p.Border
        BorderLight = p.BorderLight
        Field = p.Field
        FieldBorder = p.FieldBorder
        TextPrimary = p.TextPrimary
        TextSecondary = p.TextSecondary
        TextMuted = p.TextMuted
        TextFaint = p.TextFaint
        Green = p.Green
        Mint = p.Mint
        Red = p.Red
        Amber = p.Amber
        Purple = p.Purple
        Emerald = p.Green
        GradientStops = p.gradient
        BannerTint.applyTheme(dark)
    }
}

/** Pastel badge bg/text pairs for status pills — same pastel-on-dark-text pairs in both themes. */
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

/** Icon-chip bg/icon pairs for KPI stat tiles (Home) — reactive so tiles read in both themes. */
object IconTint {
    data class Tint(val bg: Color, val icon: Color)

    // Translucent-white chips in dark, pastel chips in light — kept simple: derive from Brand.
    val Amber get() = Tint(chip(), Brand.Amber)
    val Red get() = Tint(chip(), Brand.Red)
    val Purple get() = Tint(chip(), Brand.Purple)
    val Blue get() = Tint(chip(), if (Brand.isDark) Brand.Sky else Brand.Premier)
    val Green get() = Tint(chip(), Brand.Green)
    val Emerald get() = Tint(chip(), Brand.Green)

    private fun chip() = if (Brand.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFEAF1FF)
}

/** Translucent alert-banner colors — reactive dark/light like [Brand]. */
object BannerTint {
    data class Tint(val bg: Color, val border: Color, val text: Color)

    private object DarkPalette {
        val Error = Tint(Color(0xFF7F1D1D).copy(alpha = 0.45f), Color(0xFFF87171).copy(alpha = 0.6f), Color(0xFFFCA5A5))
        val Success = Tint(Color(0xFF065F46).copy(alpha = 0.45f), Color(0xFF34D399).copy(alpha = 0.6f), Color(0xFF6EE7B7))
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
    val base = if (Brand.isDark) darkColorScheme() else lightColorScheme()
    val colorScheme = base.copy(
        primary = Brand.Primary,
        onPrimary = Color.White,
        primaryContainer = Brand.SurfaceStrong,
        onPrimaryContainer = Brand.TextPrimary,
        inversePrimary = Brand.Sky,

        secondary = Brand.Primary,
        onSecondary = Color.White,
        secondaryContainer = Brand.Border,
        onSecondaryContainer = Brand.TextPrimary,
        tertiary = Brand.Purple,
        onTertiary = Color.White,

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

        surfaceContainer = Brand.Background,
        surfaceContainerHigh = Brand.SurfaceStrong,
        surfaceContainerHighest = Brand.SurfaceStrong,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HrmsTypography,
        shapes = HrmsShapes,
        content = content,
    )
}

/**
 * The signature Premier Blue backdrop. In dark mode it renders the navy → premier → azure diagonal
 * gradient (iOS `Theme.backdrop`); in light mode a soft light-blue near-flat surface. Every screen
 * renders inside this.
 */
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
