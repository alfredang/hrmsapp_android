package com.tertiaryinfotech.hrportal.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The app's spacing scale — mirrors the web's dominant Tailwind gap/padding steps documented in
 * DESIGN_SYSTEM.md §3 (`p-4`/`gap-4` ≈ 16dp as the workhorse unit, `p-6`/`gap-6` ≈ 24dp reserved
 * for spacious/section-level layout, `p-3`/`gap-3` ≈ 12dp for tighter stat-grids). Every screen
 * composable should reference these instead of ad-hoc `.dp` literals.
 */
object Spacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
}
