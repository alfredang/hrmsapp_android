package com.tertiaryinfotech.hrportal.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import com.tertiaryinfotech.hrportal.data.Accreditation
import com.tertiaryinfotech.hrportal.data.TagCount
import com.tertiaryinfotech.hrportal.data.TrainerSearch
import com.tertiaryinfotech.hrportal.ui.components.CollapsibleSection
import com.tertiaryinfotech.hrportal.ui.components.TintButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Trainer directory (Trainer Admins only — `summary.canManageTrainers`; the server answers 403 for
 * everyone else), rebuilt to the web's Trainer Management (464200b) so a trainer is easy to find by
 * skill and certification and easy to reach:
 *
 *  - one search box with a suggestion list of skills / certifications and their trainer counts;
 *    picking one adds it as a removable filter chip (only the chosen ones are shown);
 *  - an Accreditation menu (ACLP / ACTA / DACE / DDDLP / Finance background / IBF Fit);
 *  - Trainers / Applicants / All;
 *  - compact rows (name, accreditation badges, the matching expertise first) with a contact button
 *    for call, WhatsApp, email and copy.
 *
 * The directory is fetched once (`status=all`, no NRIC in the list) and filtered on the device with
 * [TrainerSearch], the port of the web's filter.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainersScreen(nav: NavController) {
    var search by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(TrainerStatusFilter.TRAINERS.name) }
    var tags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var accreditation by rememberSaveable { mutableStateOf(Accreditation.ANY.name) }
    var contact by remember { mutableStateOf<TrainerListItem?>(null) }

    AsyncListScreen(
        fetch = { HrmsApi.trainers(TrainerQuery(status = TrainerStatusFilter.ALL)).trainers },
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
    ) { all ->
        val acc = Accreditation.valueOf(accreditation)
        val query = acc.applyTo(TrainerQuery(q = search, tags = tags.toSet(), status = TrainerStatusFilter.valueOf(status)))
        val pool = all.filterNot(TrainerSearch::isApplicant)
        val vocab = TrainerSearch.vocabulary(pool)
        val results = TrainerSearch.filter(all, query)
        val needles = tags + search.split(' ')

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Trainers", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp,
                        modifier = Modifier.weight(1f))
                    Text("${pool.size} trainers · ${all.size - pool.size} applicants", color = Brand.TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp))
                }
                SkillSearch(
                    value = search, onChange = { search = it }, vocab = vocab, selected = tags,
                    onPick = { tag -> tags = tags + tag; search = "" },
                )
                SegmentedPicker(
                    options = TrainerStatusFilter.entries.map { it.name to it.label },
                    selected = status,
                    onSelect = { status = it },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccreditationMenu(acc, pool) { accreditation = it.name }
                    val anyFilter = search.isNotBlank() || tags.isNotEmpty() || acc != Accreditation.ANY ||
                        status != TrainerStatusFilter.TRAINERS.name
                    Box(Modifier.weight(1f))
                    if (anyFilter) {
                        Text(
                            "Clear all", color = Brand.Accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                search = ""; tags = emptyList(); accreditation = Accreditation.ANY.name
                                status = TrainerStatusFilter.TRAINERS.name
                            }.padding(horizontal = 10.dp, vertical = 10.dp),
                        )
                    }
                }
                if (tags.isNotEmpty() || acc != Accreditation.ANY) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (acc != Accreditation.ANY) RemovableChip(acc.short, gold = true) { accreditation = Accreditation.ANY.name }
                        tags.forEach { t -> RemovableChip(t) { tags = tags - t } }
                    }
                }
                Text(
                    "${results.size} ${if (results.size == 1) "match" else "matches"}",
                    color = Brand.TextSecondary, fontSize = 13.sp,
                )
            }
        }
        item { Spacer12() }
        if (results.isEmpty()) {
            item { EmptyHint(Icons.Filled.School, "No trainers match. Remove a filter or try another skill.") }
        } else {
            // Lazy rows drawn as one grouped card: round the first and last, hairlines between.
            itemsIndexed(results, key = { _, t -> t.id }) { i, t ->
                val r = Brand.CardCorner.dp
                val shape = RoundedCornerShape(
                    topStart = if (i == 0) r else 0.dp, topEnd = if (i == 0) r else 0.dp,
                    bottomStart = if (i == results.lastIndex) r else 0.dp, bottomEnd = if (i == results.lastIndex) r else 0.dp,
                )
                Column(modifier = Modifier.fillMaxWidth().clip(shape).background(Brand.Surface)) {
                    if (i > 0) RowDivider()
                    TrainerRow(t, needles, onContact = { contact = t }) { nav.navigate("trainer_profile/${t.id}") }
                }
            }
        }
    }

    contact?.let { t ->
        ContactSheet(
            name = t.name, phone = t.phone, email = t.email,
            onProfile = { contact = null; nav.navigate("trainer_profile/${t.id}") },
            onDismiss = { contact = null },
        )
    }
}

/** Search field with a suggestion list of skills and certifications (with trainer counts). */
@Composable
private fun SkillSearch(
    value: String,
    onChange: (String) -> Unit,
    vocab: List<TagCount>,
    selected: List<String>,
    onPick: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val items = TrainerSearch.suggestions(vocab, value, selected, limit = if (value.isBlank()) 6 else 8)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SearchBar(
            value = value,
            onChange = { onChange(it); dismissed = false },
            placeholder = "Name, skill, certification or course",
            onFocus = { focused = it; if (it) dismissed = false },
            onSearch = { dismissed = true; focus.clearFocus() },
        )
        AnimatedVisibility(visible = focused && !dismissed && items.isNotEmpty()) {
            Card(padding = 0, containerColor = Brand.Dialog) {
                Text(
                    (if (value.isBlank()) "Popular skills and certifications" else "Filter by skill or certification").uppercase(),
                    color = Brand.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
                )
                items.forEach { v ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(v.tag); dismissed = true }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(18.dp))
                        Text(v.tag, color = Brand.TextPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                        Text(v.kind.label, color = Brand.TextMuted, fontSize = 11.sp)
                        Text("${v.count}", color = Brand.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    if (value.isBlank()) "Or type a name, course or anything on the profile."
                    else "Tap one to add it as a filter, or keep typing to search all text.",
                    color = Brand.TextMuted, fontSize = 11.sp,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 10.dp),
                )
            }
        }
    }
}

/** "Accreditation ▾" button with a menu of the six accreditation filters and their counts. */
@Composable
private fun AccreditationMenu(current: Accreditation, pool: List<TrainerListItem>, onSelect: (Accreditation) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        val on = current != Accreditation.ANY
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(CircleShape)
                .background(if (on) Brand.SurfaceStrong else Brand.Surface)
                .border(1.dp, if (on) Brand.Accent else Brand.Border, CircleShape)
                .clickable { open = true }
                .padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(18.dp))
            Text(
                if (on) current.short else "Accreditation", color = Brand.TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Brand.TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Brand.Dialog) {
            Accreditation.entries.forEach { a ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(a.label, color = Brand.TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text("${a.count(pool)}", color = Brand.TextMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp))
                        }
                    },
                    leadingIcon = {
                        Icon(
                            if (a == current) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null, tint = if (a == current) Brand.Accent else Brand.TextMuted,
                        )
                    },
                    onClick = { onSelect(a); open = false },
                )
            }
        }
    }
}

/** A chosen filter with an × to remove it. Gold for the accreditation filter. */
@Composable
private fun RemovableChip(label: String, gold: Boolean = false, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(if (gold) Brand.Gold else Brand.SurfaceStrong)
            .border(1.dp, if (gold) Brand.Gold600 else Brand.BorderLight, CircleShape)
            .clickable(onClickLabel = "Remove $label", onClick = onRemove)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Brand.Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Icon(Icons.Filled.Close, contentDescription = "Remove $label", tint = Brand.Navy, modifier = Modifier.padding(start = 4.dp).size(16.dp))
    }
}

/** Compact directory row: name, accreditation badges, then the matching expertise first. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainerRow(t: TrainerListItem, needles: List<String>, onContact: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(initialsOf(t.name), 38)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                // Names are stored uppercase by design; shown as-is.
                Text(
                    t.name, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                t.trainingCerts.take(3).forEach { AccreditationBadge(it) }
                if (TrainerSearch.isApplicant(t)) FlagBadge("Applicant")
            }
            val expertise = TrainerSearch.orderByMatch((t.certifications + t.skills).distinct(), needles)
            val flags = listOfNotNull(if (t.financeBackground) "Finance" else null, if (t.ibfFitDeclaration) "IBF Fit" else null)
            // Matching expertise leads, so the reason a trainer matched is the first thing you read.
            val line = (expertise + flags).joinToString(" · ")
            Text(
                line.ifEmpty { lastClassLine(t.lastClassDate, t.classesCount) },
                color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onContact, enabled = !t.phone.isNullOrBlank() || !t.email.isNullOrBlank()) {
            Icon(
                Icons.Filled.ContactPhone, contentDescription = "Contact ${t.name}",
                tint = if (t.phone.isNullOrBlank() && t.email.isNullOrBlank()) Brand.TextFaint else Brand.Accent,
            )
        }
    }
}

/** Call / WhatsApp / email / copy for one trainer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactSheet(name: String, phone: String?, email: String?, onProfile: (() -> Unit)?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Brand.Dialog) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(name, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
            ContactActions(phone, email, context)
            if (onProfile != null) {
                SheetAction(Icons.Filled.Person, "View full profile", null, onProfile)
            }
        }
    }
}

/** The contact action rows shared by the directory sheet and the profile. */
@Composable
private fun ContactActions(phone: String?, email: String?, context: Context) {
    if (!phone.isNullOrBlank()) {
        SheetAction(Icons.Filled.Phone, "Call", phone) { open(context, TrainerFormat.telUri(phone), Intent.ACTION_DIAL) }
        TrainerSearch.whatsAppUrl(phone)?.let { url ->
            SheetAction(Icons.AutoMirrored.Filled.Chat, "WhatsApp", url.removePrefix("https://")) { open(context, url) }
        }
        SheetAction(Icons.Filled.ContentCopy, "Copy phone number", null) { copy(context, "Phone", phone) }
    }
    if (!email.isNullOrBlank()) {
        SheetAction(Icons.Filled.Email, "Email", email) { open(context, "mailto:$email", Intent.ACTION_SENDTO) }
        SheetAction(Icons.Filled.ContentCopy, "Copy email", null) { copy(context, "Email", email) }
    }
    if (phone.isNullOrBlank() && email.isNullOrBlank()) {
        Text("No phone or email on file.", color = Brand.TextMuted, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun SheetAction(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Brand.SmallCorner.dp)).clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(Brand.SurfaceStrong),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(20.dp)) }
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(title, color = Brand.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = Brand.TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun copy(context: Context, label: String, value: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager ?: return
    cm.setPrimaryClip(android.content.ClipData.newPlainText(label, value))
    // Android 13+ shows its own clipboard confirmation.
    if (android.os.Build.VERSION.SDK_INT < 33) {
        android.widget.Toast.makeText(context, "$label copied", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/** One trainer's full profile, in iOS grouped sections. NRIC is masked until "Show". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainerProfileScreen(id: String) {
    val context = LocalContext.current
    AsyncListScreen(fetch = { HrmsApi.trainer(id) }) { t ->
        item { ProfileHero(t) }
        if (!t.phone.isNullOrBlank() || !t.email.isNullOrBlank()) {
            item { Spacer12() }
            item { QuickContact(t, context) }
        }

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
        // Long lists fold away so the profile stays short; tap a heading to open it.
        if (t.experience.isNotEmpty()) {
            item {
                FoldSection("Work experience", t.experience.size) {
                    Card {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            t.experience.forEach { Bullet(it) }
                        }
                    }
                }
            }
        }
        val courses = t.courses.orEmpty().filter { it.title.isNotBlank() }
        item {
            FoldSection("Courses taught", courses.size) {
                if (courses.isEmpty()) EmptyCard("No courses on record.")
                else GroupedCard(courses) { CourseRow(it) }
            }
        }
        val recent = t.recentClasses.orEmpty().filter { it.title.isNotBlank() }
        val calendar = t.calendarClasses.orEmpty().filter { it.title.isNotBlank() }
        item {
            FoldSection("Recent classes", recent.size.takeIf { it > 0 } ?: calendar.size) {
                when {
                    recent.isNotEmpty() -> GroupedCard(recent) { ClassRow(it) }
                    calendar.isNotEmpty() -> GroupedCard(calendar) { CalendarClassRow(it) }
                    else -> EmptyCard("No recent classes.")
                }
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
            InfoRow("Phone", t.phone, onClick = { open(context, TrainerFormat.telUri(t.phone), Intent.ACTION_DIAL) }) {
                CopyIcon { copy(context, "Phone", t.phone) }
            }
        }
        if (!t.email.isNullOrBlank()) add {
            InfoRow("Email", t.email, onClick = { open(context, "mailto:${t.email}", Intent.ACTION_SENDTO) }) {
                CopyIcon { copy(context, "Email", t.email) }
            }
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

/** Call / WhatsApp / Email buttons right under the profile hero. */
@Composable
private fun QuickContact(t: TrainerProfile, context: Context) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val phone = t.phone
        val email = t.email
        if (!phone.isNullOrBlank()) {
            TintButton("Call", Icons.Filled.Phone, Brand.Accent, modifier = Modifier.weight(1f)) {
                open(context, TrainerFormat.telUri(phone), Intent.ACTION_DIAL)
            }
            TrainerSearch.whatsAppUrl(phone)?.let { url ->
                TintButton("WhatsApp", Icons.AutoMirrored.Filled.Chat, Brand.Green, modifier = Modifier.weight(1f)) { open(context, url) }
            }
        }
        if (!email.isNullOrBlank()) {
            TintButton("Email", Icons.Filled.Email, Brand.Accent, modifier = Modifier.weight(1f)) {
                open(context, "mailto:$email", Intent.ACTION_SENDTO)
            }
        }
    }
}

/** Grouped-section header that folds its card; opens by default when the list is short. */
@Composable
private fun FoldSection(title: String, count: Int, content: @Composable () -> Unit) {
    Box(Modifier.padding(top = 14.dp)) {
        CollapsibleSection(title, count = count, initiallyExpanded = count <= 3) { content() }
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
private fun CopyIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = Brand.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SearchBar(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    onFocus: (Boolean) -> Unit = {},
    onSearch: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brand.Surface)
            .border(1.dp, Brand.Border, RoundedCornerShape(12.dp))
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
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { onFocus(it.isFocused) },
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
