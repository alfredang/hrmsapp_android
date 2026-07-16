package com.tertiaryinfotech.hrportal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.R

/**
 * Inter — the web app's font (`next/font/google` Inter in `layout.tsx`). Bundled as a single
 * variable TTF (`res/font/inter_variable.ttf`, from Google's OFL-licensed google/fonts repo) so
 * text renders identically to the web app without a network-dependent downloadable-font provider.
 */
@OptIn(ExperimentalTextApi::class)
private fun interWeight(weight: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val InterFamily = FontFamily(
    interWeight(400),
    interWeight(500),
    interWeight(600),
    interWeight(700),
)

/**
 * Explicit sizes/weights per MD3 type role — mirrors DESIGN_SYSTEM.md §2's scale (`text-xs`
 * 11–12px, `text-sm` 14px, `text-base` 16px, `text-lg`/`text-xl` section headers, `text-2xl` KPI/
 * card titles, `text-3xl`+ hero) and its weight vocabulary (`font-normal`/`font-medium`/
 * `font-semibold`/`font-bold`). Screens must read sizes from `MaterialTheme.typography.*` rather
 * than hardcoding `fontSize = N.sp` — this is the single place that scale is allowed to live.
 *
 * `titleSmall` (15sp) and `bodySmall` (13sp) sit a notch between the web's `text-sm`/`text-xs`
 * buckets deliberately — Android's viewing distance and the app's already-tuned screens (verified
 * live against production data this session) read better at these in-between sizes than a literal
 * 14/12sp snap would.
 */
val HrmsTypography = Typography(
    displayLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    displayMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    displaySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp),

    headlineLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp), // text-2xl hero
    headlineSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp), // text-xl

    titleLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp), // text-lg
    titleMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp), // card titles
    titleSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp), // emphasis subtitle

    bodyLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp), // text-base
    bodyMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp), // text-sm
    bodySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp), // field labels/links

    labelLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp), // button text
    labelMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp), // text-xs captions
)
