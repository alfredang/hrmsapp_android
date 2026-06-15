package com.tertiaryinfotech.hrportal.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Spacer12() = Spacer(Modifier.height(12.dp))

@Composable
fun Spacer18() = Spacer(Modifier.height(18.dp))

@Composable
fun Spacer14() = Spacer(Modifier.height(14.dp))

/** A white section heading, matching the iOS `.headline` section titles. */
@Composable
fun SectionTitle(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
}
