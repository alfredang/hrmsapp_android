package com.tertiaryinfotech.hrportal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
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

val HrmsTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = InterFamily),
        displayMedium = base.displayMedium.copy(fontFamily = InterFamily),
        displaySmall = base.displaySmall.copy(fontFamily = InterFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = InterFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = InterFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = InterFamily),
        titleLarge = base.titleLarge.copy(fontFamily = InterFamily),
        titleMedium = base.titleMedium.copy(fontFamily = InterFamily),
        titleSmall = base.titleSmall.copy(fontFamily = InterFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = InterFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = InterFamily),
        bodySmall = base.bodySmall.copy(fontFamily = InterFamily),
        labelLarge = base.labelLarge.copy(fontFamily = InterFamily),
        labelMedium = base.labelMedium.copy(fontFamily = InterFamily),
        labelSmall = base.labelSmall.copy(fontFamily = InterFamily),
    )
}
