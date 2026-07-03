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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tertiaryinfotech.hrportal.ui.theme.BannerTint
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * The brand mark: an indigo rounded tile, plus the wordmark. Mirrors the web login page's
 * `w-14 h-14 bg-primary rounded-2xl` logo fallback tile (`login/page.tsx:260-263`) — the web app
 * shows a real uploaded logo image when configured, initials otherwise; this app always uses the
 * initials form to avoid a network image-loading dependency.
 */
@Composable
fun BrandHeader(compact: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 56.dp else 68.dp)
                .background(Brand.Primary, RoundedCornerShape(Brand.LogoCorner.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "TI",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 20.sp else 24.sp,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Welcome back",
                fontSize = if (compact) 20.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "Sign in to Tertiary Infotech Acadmey HR Portal",
                fontSize = 14.sp,
                color = Brand.TextSecondary,
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
            .background(Brand.Primary)
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

/**
 * Inline status banner (error / success) — mirrors the web login page's exact banner classes:
 * `bg-red-950/50 border-red-800 text-red-400` and `bg-green-950/50 border-green-800 text-green-400`
 * (`login/page.tsx:273-283`).
 */
@Composable
fun StatusBanner(isError: Boolean, text: String) {
    val tint = if (isError) BannerTint.Error else BannerTint.Success
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Brand.Corner.dp))
            .background(tint.bg)
            .border(1.dp, tint.border, RoundedCornerShape(Brand.Corner.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (isError) Icons.Filled.Error else Icons.Filled.MarkEmailRead,
            contentDescription = null, tint = tint.text, modifier = Modifier.size(20.dp),
        )
        Text(text, color = tint.text, fontSize = 13.sp)
    }
}

/** An avatar chip — renders [avatarUrl] via Coil when present, otherwise a circle of initials
 *  on the indigo brand accent (also the loading/error fallback for a broken image URL). */
@Composable
fun InitialsAvatar(initials: String, size: Int, avatarUrl: String? = null) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Brand.Primary),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp).clip(CircleShape),
            )
        } else {
            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size / 2.6f).sp)
        }
    }
}

/**
 * The shared top-bar actions row for the app's primary tab screens — a notification bell with
 * an unread badge, and a profile avatar — mirrors the web header's `NotificationBell` +
 * account menu (`header.tsx:92-98`), the persistent chrome shown on every dashboard page.
 */
@Composable
fun TopBarActions(initials: String, unreadCount: Int, onBellClick: () -> Unit, onProfileClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            IconButton(onClick = onBellClick) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color.White)
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
        IconButton(onClick = onProfileClick) {
            InitialsAvatar(initials, 30)
        }
    }
}
