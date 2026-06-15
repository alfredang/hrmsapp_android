package com.tertiaryinfotech.hrportal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/** Apply [value] alpha only when [condition] is true; otherwise leave fully opaque. */
fun Modifier.alphaIf(condition: Boolean, value: Float): Modifier =
    if (condition) this.alpha(value) else this

/** Make the element clickable only when [enabled]. */
fun Modifier.clickableIf(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.clickable(onClick = onClick) else this
