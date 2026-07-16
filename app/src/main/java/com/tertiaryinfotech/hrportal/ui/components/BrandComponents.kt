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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tertiaryinfotech.hrportal.data.BrandingResponse
import com.tertiaryinfotech.hrportal.data.Net
import com.tertiaryinfotech.hrportal.ui.theme.BannerTint
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.ui.theme.Spacing

/**
 * The brand mark: a rounded logo tile plus the wordmark. Mirrors the web login page's real-logo-
 * or-initials-fallback tile (`login/page.tsx:260-263`, `w-14 h-14 bg-primary rounded-2xl` when no
 * logo is configured) — [branding] drives the same behavior here: a real uploaded logo image via
 * Coil when [BrandingResponse.logo] is set, an initials tile otherwise. [loading] shows the
 * pulsing skeleton block described in SCREEN_MAP.md's Login `LoginSkeleton` while the
 * unauthenticated `GET /api/public/branding` fetch is in flight.
 */
@Composable
fun BrandHeader(
    compact: Boolean = false,
    branding: BrandingResponse? = null,
    loading: Boolean = false,
    title: String = "Welcome back",
    subtitle: String = "Sign in to Tertiary Infotech Academy HR Portal",
) {
    val tileSize = if (compact) 56.dp else 68.dp
    val titleStyle = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) Spacing.md else Spacing.lg),
    ) {
        if (loading) {
            PulseBlock(modifier = Modifier.size(tileSize), shape = RoundedCornerShape(Brand.LogoCorner.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(tileSize)
                    .background(Brand.Primary, RoundedCornerShape(Brand.LogoCorner.dp)),
                contentAlignment = Alignment.Center,
            ) {
                // The backend returns a host-relative path (e.g. "/branding/company-logo.png"),
                // not an absolute URL — resolve it against Net.BASE_URL the same way Net.url()
                // resolves the PDF-download path, otherwise Coil silently fails to load it.
                val logoUrl = branding?.logo?.takeIf { it.isNotBlank() }?.let { Net.url(it) }
                var logoFailed by remember(logoUrl) { mutableStateOf(false) }
                if (logoUrl != null && !logoFailed) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onError = { logoFailed = true },
                        modifier = Modifier.size(tileSize).clip(RoundedCornerShape(Brand.LogoCorner.dp)),
                    )
                } else {
                    Text(
                        branding?.initials ?: "TI",
                        color = Brand.TextPrimary,
                        style = titleStyle,
                    )
                }
            }
        }
        if (loading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PulseBlock(modifier = Modifier.width(160.dp).height(Spacing.xl))
                PulseBlock(modifier = Modifier.width(220.dp).height(Spacing.lg))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(title, style = titleStyle, color = Brand.TextPrimary)
                Text(
                    branding?.let { "Sign in to ${it.displayName} HR Portal" } ?: subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Brand.TextSecondary,
                )
            }
        }
    }
}

/**
 * A branded primary button with a working spinner. Full-width (>=56dp tall) by default for
 * hero/form-submit CTAs; pass [fullWidth] = false for compact, content-sized usage (e.g. sitting
 * next to a Cancel button in a right-aligned footer row) instead of stretching edge-to-edge.
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
    Box(
        modifier = modifier
            .let { if (fullWidth) it.fillMaxWidth() else it }
            .height(if (fullWidth) Brand.ControlHeight.dp else 44.dp)
            .clip(RoundedCornerShape(Brand.Corner.dp))
            .background(Brand.Primary)
            .alphaIf(!active, 0.55f)
            .clickableIf(active, onClick)
            .padding(horizontal = if (fullWidth) 0.dp else Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        // Always on the indigo Brand.Primary fill (unchanged across themes), so this stays white
        // rather than following the theme-reactive Brand.TextPrimary.
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (icon != null) Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(title, color = Color.White, style = MaterialTheme.typography.labelLarge)
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
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            if (isError) Icons.Filled.Error else Icons.Filled.MarkEmailRead,
            contentDescription = null, tint = tint.text, modifier = Modifier.size(20.dp),
        )
        Text(text, color = tint.text, style = MaterialTheme.typography.bodySmall)
    }
}

/** An avatar chip — renders [avatarUrl] via Coil when present, otherwise a circle of initials
 *  on the indigo brand accent (also the loading/error fallback for a broken or host-relative
 *  image URL — the backend mixes relative upload paths and absolute Google-avatar URLs across
 *  different fields, so this resolves via [Net.url] and falls back to initials on load failure,
 *  same as [BrandHeader]'s logo handling). */
@Composable
fun InitialsAvatar(initials: String, size: Int, avatarUrl: String? = null) {
    val resolvedUrl = avatarUrl?.takeIf { it.isNotBlank() }?.let { Net.url(it) }
    var avatarFailed by remember(resolvedUrl) { mutableStateOf(false) }
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Brand.Primary),
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

/**
 * The shared top-bar actions row for the app's primary tab screens — a notification bell with
 * an unread badge, and a profile avatar that opens a small account menu (name/email header, My
 * Profile, Sign out) — mirrors the web header's `NotificationBell` + `user-nav.tsx` account
 * dropdown, the persistent chrome shown on every dashboard page.
 */
@Composable
fun TopBarActions(
    initials: String,
    displayName: String,
    email: String,
    avatarUrl: String?,
    unreadCount: Int,
    onBellClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSignOut: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            IconButton(onClick = onBellClick) {
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
        var menuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                InitialsAvatar(initials, 30, avatarUrl)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                    Text(displayName, color = Brand.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(email, color = Brand.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                HorizontalDivider(color = Brand.Border)
                DropdownMenuItem(
                    text = { Text("My Profile") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    onClick = { menuExpanded = false; onProfileClick() },
                )
                DropdownMenuItem(
                    text = { Text("Sign out", color = Brand.Red) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Brand.Red) },
                    onClick = { menuExpanded = false; onSignOut() },
                )
            }
        }
    }
}
