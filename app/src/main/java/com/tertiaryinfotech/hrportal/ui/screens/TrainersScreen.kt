package com.tertiaryinfotech.hrportal.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.TrainerCalendarClass
import com.tertiaryinfotech.hrportal.data.TrainerClass
import com.tertiaryinfotech.hrportal.data.TrainerCourse
import com.tertiaryinfotech.hrportal.data.TrainerFormat
import com.tertiaryinfotech.hrportal.data.TrainerListItem
import com.tertiaryinfotech.hrportal.data.TrainerProfile
import com.tertiaryinfotech.hrportal.data.TrainerQuery
import com.tertiaryinfotech.hrportal.data.TrainerStatusFilter
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.components.InitialsAvatar
import com.tertiaryinfotech.hrportal.ui.components.StatusPill
import com.tertiaryinfotech.hrportal.ui.theme.Brand
import com.tertiaryinfotech.hrportal.util.Fmt
import kotlinx.coroutines.delay

/**
 * Trainer directory (Trainer Admins only — `summary.canManageTrainers`; the server answers 403 for
 * everyone else). Search, an iOS segmented control for Trainers / Applicants / All, and accreditation
 * filter pills. The server does the matching; this screen only sends the query.
 */
@Composable
fun TrainersScreen(nav: NavController) {
    var search by rememberSaveable { mutableStateOf("") }
    var debounced by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(search) {
        delay(350)
        debounced = search
    }
    var status by rememberSaveable { mutableStateOf(TrainerStatusFilter.TRAINERS.name) }
    var certs by rememberSaveable { mutableStateOf(listOf<String>()) }
    var finance by rememberSaveable { mutableStateOf(false) }
    var ibfFit by rememberSaveable { mutableStateOf(false) }

    val query = TrainerQuery(
        q = debounced, certs = certs.toSet(), finance = finance, ibfFit = ibfFit,
        status = TrainerStatusFilter.valueOf(status),
    )

    Column {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Trainers", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 30.sp)
            SearchBar(search, { search = it }, "Name, skill, cert or course")
            SegmentedPicker(
                options = TrainerStatusFilter.entries.map { it.name to it.label },
                selected = status,
                onSelect = { status = it },
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrainerFormat.TRAINING_CERTS.forEach { c ->
                    FilterPill(c, c in certs) { certs = if (c in certs) certs - c else certs + c }
                }
                FilterPill("Finance", finance) { finance = !finance }
                FilterPill("IBF Fit", ibfFit) { ibfFit = !ibfFit }
            }
        }

        AsyncListScreen(
            fetch = { HrmsApi.trainers(query) },
            key = query,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        ) { data ->
            item {
                Text(
                    "${data.count} ${if (data.count == 1) "trainer" else "trainers"}",
                    color = Brand.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            if (data.trainers.isEmpty()) {
                item { EmptyHint(Icons.Filled.School, "No trainers match these filters.") }
            } else {
                items(data.trainers, key = { it.id }) { t ->
                    TrainerRow(t) { nav.navigate("trainer_profile/${t.id}") }
                    Spacer12()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainerRow(t: TrainerListItem, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(initialsOf(t.name), 44)
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                // Names are stored uppercase by design; shown as-is.
                Text(t.name, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!t.nickname.isNullOrBlank()) {
                    Text("“${t.nickname}”", color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 1)
                }
                val hasBadges = t.trainingCerts.isNotEmpty() || t.financeBackground || t.ibfFitDeclaration
                if (hasBadges) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        t.trainingCerts.forEach { AccreditationBadge(it) }
                        if (t.financeBackground) FlagBadge("Finance")
                        if (t.ibfFitDeclaration) FlagBadge("IBF Fit")
                    }
                }
                val tags = t.topTags()
                if (tags.isNotEmpty()) {
                    Text(tags.joinToString(" · "), color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(lastClassLine(t.lastClassDate, t.classesCount), color = Brand.TextMuted, fontSize = 11.sp)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** One trainer's full profile, in iOS grouped sections. NRIC is masked until "Show". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainerProfileScreen(id: String) {
    val context = LocalContext.current
    AsyncListScreen(fetch = { HrmsApi.trainer(id) }) { t ->
        item { ProfileHero(t) }

        item { GroupHeader("Identity & contact") }
        item { IdentityCard(t, context) }

        item { GroupHeader("Accreditation") }
        item { AccreditationCard(t) }

        if (t.certifications.isNotEmpty()) {
            item { GroupHeader("Certifications") }
            item { Card { TagFlow(t.certifications) } }
        }
        if (!t.education.isNullOrBlank()) {
            item { GroupHeader("Education") }
            item { Card { Text(t.education, color = Brand.TextPrimary, fontSize = 14.sp) } }
        }
        if (t.skills.isNotEmpty()) {
            item { GroupHeader("Skills") }
            item { Card { TagFlow(t.skills) } }
        }
        if (t.experience.isNotEmpty()) {
            item { GroupHeader("Work experience") }
            item {
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        t.experience.forEach { Bullet(it) }
                    }
                }
            }
        }
        val courses = t.courses.orEmpty().filter { it.title.isNotBlank() }
        item { GroupHeader("Courses taught") }
        item {
            if (courses.isEmpty()) EmptyCard("No courses on record.")
            else GroupedCard(courses) { CourseRow(it) }
        }
        val recent = t.recentClasses.orEmpty().filter { it.title.isNotBlank() }
        val calendar = t.calendarClasses.orEmpty().filter { it.title.isNotBlank() }
        item { GroupHeader("Recent classes") }
        item {
            when {
                recent.isNotEmpty() -> GroupedCard(recent) { ClassRow(it) }
                calendar.isNotEmpty() -> GroupedCard(calendar) { CalendarClassRow(it) }
                else -> EmptyCard("No recent classes.")
            }
        }
        if (!t.adminNotes.isNullOrBlank()) {
            item { GroupHeader("Admin notes") }
            item { Card { Text(t.adminNotes, color = Brand.TextPrimary, fontSize = 14.sp) } }
        }
        item { Spacer18() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHero(t: TrainerProfile) {
    Card(containerColor = Brand.SurfaceStrong) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(initialsOf(t.name), 60)
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t.name, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                if (!t.nickname.isNullOrBlank()) Text("“${t.nickname}”", color = Brand.TextSecondary, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (t.status.isNotBlank()) StatusPill(t.status)
                    if (!t.trainerType.isNullOrBlank()) FlagBadge(t.trainerType)
                    if (t.inTms) FlagBadge("In TMS")
                }
            }
        }
        Text(
            lastClassLine(t.lastClassDate, t.classesCount), color = Brand.TextSecondary, fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (!t.summary.isNullOrBlank()) {
            Text(t.summary, color = Brand.TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun IdentityCard(t: TrainerProfile, context: Context) {
    var showNric by remember { mutableStateOf(false) }
    val rows = buildList<@Composable () -> Unit> {
        if (!t.nric.isNullOrBlank()) add {
            InfoRow("NRIC", if (showNric) t.nric else TrainerFormat.maskNric(t.nric)) {
                Text(
                    if (showNric) "Hide" else "Show", color = Brand.Accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showNric = !showNric }.padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        if (!t.phone.isNullOrBlank()) add {
            InfoRow("Phone", t.phone, icon = Icons.Filled.Phone, onClick = { open(context, TrainerFormat.telUri(t.phone), Intent.ACTION_DIAL) })
        }
        if (!t.email.isNullOrBlank()) add {
            InfoRow("Email", t.email, icon = Icons.Filled.Email, onClick = { open(context, "mailto:${t.email}", Intent.ACTION_SENDTO) })
        }
        if (!t.secondaryEmail.isNullOrBlank()) add {
            InfoRow("Other email", t.secondaryEmail, icon = Icons.Filled.Email, onClick = { open(context, "mailto:${t.secondaryEmail}", Intent.ACTION_SENDTO) })
        }
        if (!t.nationality.isNullOrBlank()) add { InfoRow("Nationality", t.nationality) }
        if (!t.linkedinUrl.isNullOrBlank()) add {
            InfoRow("LinkedIn", t.linkedinUrl, onClick = { open(context, TrainerFormat.webUrl(t.linkedinUrl)) })
        }
    }
    if (rows.isEmpty()) EmptyCard("No contact details on file.") else GroupedCard(rows) { it() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccreditationCard(t: TrainerProfile) {
    Card(padding = 4) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Training certificates", color = Brand.TextSecondary, fontSize = 12.sp)
            if (t.trainingCerts.isEmpty()) {
                Text("None on file", color = Brand.TextMuted, fontSize = 14.sp)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    t.trainingCerts.forEach { AccreditationBadge(it) }
                }
            }
        }
        RowDivider()
        FlagRow("Finance background", t.financeBackground, t.financeEvidence)
        RowDivider()
        FlagRow("IBF Fit & Proper declaration", t.ibfFitDeclaration, t.ibfFitFiles)
        if (!t.actaFolder.isNullOrBlank() || t.evidenceFiles.isNotEmpty()) {
            RowDivider()
            InfoRow(
                "ACTA folder",
                (t.actaFolder ?: "—") + if (t.evidenceFiles.isNotEmpty()) " · ${t.evidenceFiles.size} file(s)" else "",
            )
        }
    }
}

@Composable
private fun FlagRow(label: String, on: Boolean, evidence: List<String>) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Brand.TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(if (on) "Yes" else "No", color = if (on) Brand.Green else Brand.TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        evidence.take(4).forEach { Text(it, color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (evidence.size > 4) Text("+${evidence.size - 4} more", color = Brand.TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun CourseRow(c: TrainerCourse) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(c.title, color = Brand.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        val classes = c.classes.toInt()
        val parts = listOfNotNull(
            c.code.takeIf { it.isNotBlank() },
            if (classes > 0) "$classes ${if (classes == 1) "class" else "classes"}" else null,
            dateRange(c.first, c.last),
        )
        if (parts.isNotEmpty()) Text(parts.joinToString(" · "), color = Brand.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ClassRow(c: TrainerClass) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(c.title, color = Brand.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            val parts = listOfNotNull(
                c.start.takeIf { it.isNotBlank() }?.let { Fmt.date(it) },
                c.run.takeIf { it.isNotBlank() }?.let { "Run $it" },
            )
            if (parts.isNotEmpty()) Text(parts.joinToString(" · "), color = Brand.TextSecondary, fontSize = 12.sp)
        }
        if (c.status.isNotBlank()) StatusPill(c.status)
    }
}

@Composable
private fun CalendarClassRow(c: TrainerCalendarClass) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(c.title, color = Brand.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        val parts = listOfNotNull(c.date.takeIf { it.isNotBlank() }?.let { Fmt.date(it) }, c.calendar.takeIf { it.isNotBlank() })
        if (parts.isNotEmpty()) Text(parts.joinToString(" · "), color = Brand.TextSecondary, fontSize = 12.sp)
    }
}

// MARK: - Building blocks (iOS grouped list look)

/** iOS inset-grouped section header: small uppercase caption above the card. */
@Composable
private fun GroupHeader(text: String) {
    Text(
        text.uppercase(), color = Brand.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun <T> GroupedCard(rows: List<T>, row: @Composable (T) -> Unit) {
    Card(padding = 4) {
        rows.forEachIndexed { i, r ->
            if (i > 0) RowDivider()
            row(r)
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card { Text(text, color = Brand.TextMuted, fontSize = 14.sp) }
}

@Composable
private fun RowDivider() {
    Box(modifier = Modifier.fillMaxWidth().padding(start = 12.dp).height(1.dp).background(Brand.TextPrimary.copy(alpha = 0.08f)))
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Brand.SmallCorner.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = Brand.TextSecondary, fontSize = 12.sp)
            Text(value, color = if (onClick != null) Brand.Accent else Brand.TextPrimary, fontSize = 15.sp)
        }
        if (trailing != null) trailing()
        else if (icon != null) Icon(icon, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit, placeholder: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brand.Field)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Brand.TextMuted, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (value.isEmpty()) Text(placeholder, color = Brand.TextMuted, fontSize = 16.sp, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Brand.TextPrimary, fontSize = 16.sp),
                cursorBrush = SolidColor(Brand.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Cancel, contentDescription = "Clear search", tint = Brand.TextMuted,
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onChange("") },
            )
        }
    }
}

/** Capsule toggle (iOS-style filter token), not a Material chip. */
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) Brand.Primary else Brand.Surface)
            .border(1.dp, if (selected) Brand.Primary else Brand.Border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = if (selected) Color.White else Brand.TextPrimary, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Training-certificate badge — the brand's single gold accent, navy text. */
@Composable
private fun AccreditationBadge(text: String) {
    Box(
        modifier = Modifier.clip(CircleShape).background(Brand.Gold).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = Brand.Navy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FlagBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Brand.Accent.copy(alpha = 0.16f))
            .border(1.dp, Brand.Accent.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = Brand.Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFlow(tags: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brand.Field)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) { Text(tag, color = Brand.TextPrimary, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.padding(top = 7.dp).size(5.dp).clip(CircleShape).background(Brand.Accent))
        Text(text, color = Brand.TextPrimary, fontSize = 14.sp)
    }
}

private fun initialsOf(name: String): String =
    name.split(' ').filter { it.isNotBlank() && it.first().isLetter() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

private fun lastClassLine(lastClassDate: String?, classesCount: Int): String = when {
    lastClassDate.isNullOrBlank() && classesCount == 0 -> "No classes on record"
    lastClassDate.isNullOrBlank() -> "$classesCount ${if (classesCount == 1) "class" else "classes"}"
    else -> "Last class ${Fmt.date(lastClassDate)} · $classesCount ${if (classesCount == 1) "class" else "classes"}"
}

private fun dateRange(first: String, last: String): String? {
    val f = first.takeIf { it.isNotBlank() }?.let { Fmt.date(it) }
    val l = last.takeIf { it.isNotBlank() }?.let { Fmt.date(it) }
    return when {
        f != null && l != null && f != l -> "$f – $l"
        else -> f ?: l
    }
}

private fun open(context: Context, uri: String, action: String = Intent.ACTION_VIEW) {
    try {
        context.startActivity(Intent(action, Uri.parse(uri)))
    } catch (_: ActivityNotFoundException) { /* no app for this link */ }
}
