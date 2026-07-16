package com.tertiaryinfotech.hrportal.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner-radius scale — mirrors DESIGN_SYSTEM.md §3's border-radius buckets: `rounded-lg` (12dp,
 * the workhorse for buttons/inputs/selects — [Brand.Corner]), `rounded-xl`/`rounded-2xl` (larger
 * cards and feature panels/modals — [Brand.LogoCorner]).
 *
 * `extraLarge` is deliberately NOT a pill — native M3 components (`AlertDialog`,
 * `DatePickerDialog`, `ModalBottomSheet`) default their *container* corner radius to
 * `MaterialTheme.shapes.extraLarge`. Setting it to a 999dp pill (as a first pass here did)
 * turned every dialog into an egg/oval shape. Pills/circles (avatars, status badges) should use
 * [HrmsCircleShape] or an explicit `RoundedCornerShape(percent = 50)` at the call site instead of
 * going through the shared theme shape, since nothing else in M3 reads `shapes.extraLarge` as
 * "pill" — it reads it as "biggest rectangular corner radius."
 */
val HrmsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(Brand.Corner.dp), // buttons / inputs / fields — web `rounded-lg`
    medium = RoundedCornerShape(Brand.LogoCorner.dp), // cards / panels — web `rounded-xl`/`rounded-2xl`
    large = RoundedCornerShape(24.dp), // large modals/sheets
    extraLarge = RoundedCornerShape(28.dp), // dialog/bottom-sheet container radius (M3 baseline)
)

/** Pill/circle shape for avatars, badges, and status pills — deliberately not routed through
 *  `MaterialTheme.shapes` (see [HrmsShapes] doc). */
val HrmsCircleShape = CircleShape
