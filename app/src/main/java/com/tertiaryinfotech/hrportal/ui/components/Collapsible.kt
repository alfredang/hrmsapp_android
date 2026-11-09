package com.tertiaryinfotech.hrportal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.hrportal.ui.theme.Brand

/**
 * A titled section that folds open and shut (chevron + animated height), so long admin pages stay
 * short: only the part you are working on is open. [count] shows as a badge; [highlight] tints it
 * amber for "needs action". The open/closed state survives rotation.
 */
@Composable
fun CollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    count: Int? = null,
    highlight: Boolean = false,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val turn by animateFloatAsState(if (open) 0f else -90f, label = "chevron")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Brand.SmallCorner.dp))
                .clickable(role = Role.Button) { open = !open }
                .semantics { contentDescription = "$title, ${if (open) "expanded" else "collapsed"}" }
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(20.dp))
            }
            Text(
                title, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.padding(start = if (icon != null) 8.dp else 0.dp),
            )
            if (count != null) {
                val bg = if (highlight && count > 0) Brand.Gold else Brand.SurfaceStrong
                Box(
                    modifier = Modifier.padding(start = 8.dp).clip(CircleShape).background(bg)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("$count", color = Brand.Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.weight(1f))
            Icon(
                Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Brand.TextSecondary,
                modifier = Modifier.size(24.dp).rotate(turn),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }
}

/** A compact single-line list row inside a card: bold lead, muted middle, right-aligned trailing. */
@Composable
fun CompactLine(
    lead: String,
    middle: String,
    trailing: String,
    modifier: Modifier = Modifier,
    trailingColor: Color = Brand.TextPrimary,
    badge: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            lead, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 170.dp),
        )
        badge?.invoke()
        Text(
            middle, color = Brand.TextSecondary, fontSize = 13.sp, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Text(trailing, color = trailingColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

/** Hairline between [CompactLine]s in one card. */
@Composable
fun LineDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 14.dp).height(1.dp).background(Brand.Border))
}
