package com.tertiaryinfotech.hrportal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tertiaryinfotech.hrportal.R
import com.tertiaryinfotech.hrportal.data.Net
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.Spacing

/**
 * The brand mark from the iOS `BrandHeader`: a frosted rounded badge with the people glyph, plus the
 * wordmark. Used on the login screen and the launch splash.
 */
@Composable
fun BrandHeader(compact: Boolean = false) {
    val tile = if (compact) 76.dp else 92.dp
    val shape = RoundedCornerShape(Brand.LogoCorner.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(tile)
                .clip(shape)
                .background(if (Brand.isDark) Color.White.copy(alpha = 0.14f) else Brand.SurfaceStrong)
                .border(1.dp, if (Brand.isDark) Color.White.copy(alpha = 0.25f) else Brand.BorderLight, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Groups, contentDescription = null, tint = Brand.TextPrimary,
                modifier = Modifier.size(if (compact) 40.dp else 50.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Tertiary HRMS", color = Brand.TextPrimary, fontWeight = FontWeight.Bold,
                fontSize = if (compact) 24.sp else 30.sp,
            )
            Text(
                "Human Resource Management", color = Brand.TextPrimary.copy(alpha = 0.75f),
                fontSize = 15.sp,
            )
        }
    }
}

/** The company lockup (logo + name) shown as the top-bar title on every screen, as on iOS. */
@Composable
fun BrandBarTitle() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(
            painterResource(R.drawable.company_logo), contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(30.dp).clip(CircleShape),
        )
        Text(
            "Tertiary Infotech Academy", color = Brand.TextPrimary, fontWeight = FontWeight.Bold,
            fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The large branded primary button: azure-to-premier gradient, premier glow and semibold label.
 * [fullWidth] = false gives the compact content-sized variant for inline actions.
 */
@Composable
fun PremierButton(
    title: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    onClick: () -> Unit,
) {
    val active = enabled && !loading
    val shape = RoundedCornerShape(Brand.Corner.dp)
    Box(
        modifier = modifier
            // Alpha goes first so it dims the fill and glow too, not just the label.
            .alphaIf(!active, 0.55f)
            .let { if (fullWidth) it.fillMaxWidth() else it }
            .height(if (fullWidth) Brand.ControlHeight.dp else 44.dp)
            .shadow(if (active) 10.dp else 0.dp, shape, spotColor = Brand.Premier, ambientColor = Brand.Premier)
            .clip(shape)
            .background(Brand.AccentGradient)
            .clickableIf(active, onClick)
            .padding(horizontal = if (fullWidth) 0.dp else Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        // Always on the blue fill in both themes, so this stays white.
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (icon != null) Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(
                    title, color = Color.White, fontWeight = FontWeight.SemiBold,
                    fontSize = if (fullWidth) 17.sp else 15.sp,
                )
            }
        }
    }
}

enum class BannerKind { Error, Info, Success }

/** Inline status banner: tinted fill, coloured stroke, white text (iOS `StatusBanner`). */
@Composable
fun StatusBanner(kind: BannerKind, text: String) {
    val shape = RoundedCornerShape(Brand.SmallCorner.dp)
    val (icon, fill, stroke) = when (kind) {
        BannerKind.Error -> Triple(Icons.Filled.Warning, Brand.Red, Brand.Red)
        BannerKind.Info -> Triple(Icons.Filled.MarkEmailUnread, Brand.Premier, Brand.Accent)
        BannerKind.Success -> Triple(Icons.Filled.CheckCircle, Brand.Green, Brand.Green)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill.copy(alpha = 0.28f))
            .border(1.dp, stroke.copy(alpha = 0.5f), shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Brand.TextPrimary, modifier = Modifier.size(18.dp))
        Text(text, color = Brand.TextPrimary, fontSize = 13.sp)
    }
}

/** Existing error/success call sites. */
@Composable
fun StatusBanner(isError: Boolean, text: String) =
    StatusBanner(if (isError) BannerKind.Error else BannerKind.Success, text)

/** The white "Continue with Google" button with the multi-colour mark that Google's guidelines
 *  require, so it deliberately breaks from the blue of [PremierButton]. */
@Composable
fun GoogleSignInButton(loading: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val active = enabled && !loading
    val shape = RoundedCornerShape(Brand.Corner.dp)
    Box(
        modifier = Modifier
            .alphaIf(!active, 0.55f)
            .fillMaxWidth()
            .height(Brand.ControlHeight.dp)
            .shadow(if (active) 6.dp else 0.dp, shape)
            .clip(shape)
            .background(Color.White)
            .clickableIf(active, onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color(0xFF424242), strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.ic_google_g), contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Continue with Google", color = Color(0xFF1C1C1C), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
        }
    }
}

/** A labelled hairline divider ("or") between two sign-in options. */
@Composable
fun OrDivider(label: String = "or") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(Brand.TextPrimary.copy(alpha = 0.18f)))
        Text(label, color = Brand.TextPrimary.copy(alpha = 0.6f), fontSize = 12.sp)
        Box(Modifier.weight(1f).height(1.dp).background(Brand.TextPrimary.copy(alpha = 0.18f)))
    }
}

/** An avatar chip — renders [avatarUrl] via Coil when present, otherwise a circle of initials
 *  on the accent gradient (also the loading/error fallback for a broken or host-relative
 *  image URL — the backend mixes relative upload paths and absolute Google-avatar URLs across
 *  different fields, so this resolves via [Net.url] and falls back to initials on load failure,
 *  same as [BrandHeader]'s logo handling). */
@Composable
fun InitialsAvatar(initials: String, size: Int, avatarUrl: String? = null) {
    val resolvedUrl = avatarUrl?.takeIf { it.isNotBlank() }?.let { Net.url(it) }
    var avatarFailed by remember(resolvedUrl) { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Brand.AccentGradient),
        contentAlignment = Alignment.Center,
    ) {
        if (resolvedUrl != null && !avatarFailed) {
            AsyncImage(
                model = resolvedUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { avatarFailed = true },
                modifier = Modifier.size(size.dp).clip(CircleShape),
            )
        } else {
            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.6f).sp)
        }
    }
}

/** The notifications bell with its unread badge. Home only, as on iOS. */
@Composable
fun NotificationBell(unreadCount: Int, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Brand.TextPrimary)
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Brand.Red),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (unreadCount > 9) "9+" else "$unreadCount",
                    color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
