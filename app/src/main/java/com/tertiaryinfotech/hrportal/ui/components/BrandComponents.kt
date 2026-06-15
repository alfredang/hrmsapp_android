package com.tertiaryinfotech.hrportal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/** The Premier Blue brand mark: a rounded badge with a people glyph, plus the wordmark. */
@Composable
fun BrandHeader(compact: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 76.dp else 92.dp)
                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Group, contentDescription = null, tint = Color.White,
                modifier = Modifier.size(if (compact) 40.dp else 50.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Tertiary HRMS",
                fontSize = if (compact) 24.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "Human Resource Management",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}

/** A large, branded primary button (>=56dp) with a working spinner. */
@Composable
fun PremierButton(
    title: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val active = enabled && !loading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Brand.ControlHeight.dp)
            .clip(RoundedCornerShape(Brand.Corner.dp))
            .background(Brand.accent())
            .alphaIf(!active, 0.55f)
            .clickableIf(active, onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (icon != null) Icon(icon, contentDescription = null, tint = Color.White)
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Inline status banner (error / info) on the dark surface. */
@Composable
fun StatusBanner(isError: Boolean, text: String) {
    val accent = if (isError) Brand.Red else Brand.Premier
    val stroke = if (isError) Brand.Red else Brand.Sky
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.28f))
            .border(1.dp, stroke.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (isError) Icons.Filled.Error else Icons.Filled.MarkEmailRead,
            contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp),
        )
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}

/** An avatar chip showing initials on the Premier accent gradient. */
@Composable
fun InitialsAvatar(initials: String, size: Int) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Brand.accent()),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.6f).sp)
    }
}
