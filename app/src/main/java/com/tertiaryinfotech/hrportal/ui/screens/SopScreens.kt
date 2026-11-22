package com.tertiaryinfotech.hrportal.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tertiaryinfotech.hrportal.data.HrmsApi
import com.tertiaryinfotech.hrportal.data.Sop
import com.tertiaryinfotech.hrportal.data.SopCache
import com.tertiaryinfotech.hrportal.data.SopReference
import com.tertiaryinfotech.hrportal.data.SopRules
import com.tertiaryinfotech.hrportal.data.SopStep
import com.tertiaryinfotech.hrportal.ui.components.AsyncListScreen
import com.tertiaryinfotech.hrportal.ui.components.Card
import com.tertiaryinfotech.hrportal.ui.components.EmptyHint
import com.tertiaryinfotech.hrportal.ui.theme.Brand

// Standard Operating Procedures (web /sop), shown natively from GET /api/mobile/sops so no browser sign-in is needed
// (Alfred, 22 Nov 2026; iOS `Views/SopViews.swift`). The bottom-nav tab for everyone but admins ("sops"), and
// More → Documents → SOP for all ("sop_list"). A row opens "sop/{slug}".

/** The SOP list: header, search (every word must match), grouped by category in the server's order. */
@Composable
fun SopListScreen(nav: NavController) {
    var query by rememberSaveable { mutableStateOf("") }
    AsyncListScreen(fetch = { HrmsApi.sops() }) { all ->
        val shown = all.filter { it.matches(query) }
        val areas = all.map { it.category }.distinct().size
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.semantics { testTag = "sop.header" }) {
                Text("Standard Operating Procedures", color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(
                    "${all.size} procedures in $areas areas. Follow each SOP as written; if anything is unclear, check with your supervisor first.",
                    color = Brand.TextSecondary, fontSize = 13.sp,
                )
            }
        }
        item { Spacer14() }
        item {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search SOPs", color = Brand.TextMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Brand.TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().semantics { testTag = "sop.search" },
                shape = RoundedCornerShape(Brand.SmallCorner.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Brand.Field,
                    unfocusedContainerColor = Brand.Field,
                    focusedTextColor = Brand.TextPrimary,
                    unfocusedTextColor = Brand.TextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }
        item { Spacer18() }
        if (shown.isEmpty()) {
            item { EmptyHint(Icons.Filled.Search, if (query.isBlank()) "No SOPs yet." else "No SOP matches “$query”.") }
        }
        SopRules.orderedCategories(shown).forEach { cat ->
            item { SectionTitle(cat) }
            item { Spacer12() }
            item {
                Card(padding = 0) {
                    shown.filter { it.category == cat }.forEachIndexed { i, sop ->
                        if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 14.dp).height(1.dp).background(Brand.TextPrimary.copy(alpha = 0.08f)))
                        SopRow(sop) { nav.navigate("sop/${Uri.encode(sop.slug)}") }
                    }
                }
            }
            item { Spacer18() }
        }
    }
}

@Composable
private fun SopRow(sop: Sop, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { testTag = "sop.row.${sop.slug}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(sop.title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (sop.summary.isNotBlank()) Text(sop.summary, color = Brand.TextSecondary, fontSize = 12.sp, maxLines = 2)
            Text(sop.stepsLine, color = Brand.TextMuted, fontSize = 11.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = Brand.TextPrimary.copy(alpha = 0.4f), modifier = Modifier.padding(start = 8.dp).size(18.dp),
        )
    }
}

/**
 * One SOP: category, title and summary; Purpose / Scope / Owner / When to use; the numbered steps (who, when, details,
 * cautions, links); then the references and the source section of the manual.
 */
@Composable
fun SopDetailScreen(slug: String) {
    var sop by remember(slug) { mutableStateOf(SopCache.all.firstOrNull { it.slug == slug }) }
    var error by remember(slug) { mutableStateOf<String?>(null) }
    LaunchedEffect(slug) {
        if (sop != null) return@LaunchedEffect
        try {
            sop = HrmsApi.sops().firstOrNull { it.slug == slug }
            if (sop == null) error = "This SOP is no longer available."
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Could not load the SOPs."
        }
    }
    val s = sop
    when {
        s != null -> SopDetail(s)
        error != null -> EmptyHint(Icons.Filled.Warning, error.orEmpty())
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand.TextPrimary) }
    }
}

@Composable
private fun SopDetail(sop: Sop) {
    val context = LocalContext.current
    fun open(href: String) {
        val url = SopRules.linkUrl(href) ?: return
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) { /* nothing can open it */ }
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(sop.category.uppercase(), color = Brand.Accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(sop.title, color = Brand.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                modifier = Modifier.semantics { testTag = "sop.detail.title" })
            if (sop.summary.isNotBlank()) Text(sop.summary, color = Brand.TextPrimary.copy(alpha = 0.8f), fontSize = 15.sp)
        }

        val meta = listOf("Purpose" to sop.purpose, "Scope" to sop.scope, "Owner" to sop.owner)
            .mapNotNull { (l, v) -> v?.takeIf { it.isNotBlank() }?.let { l to it } }
        val triggers = sop.triggers.orEmpty().filter { it.isNotBlank() }
        if (meta.isNotEmpty() || triggers.isNotEmpty()) {
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    meta.forEach { (label, value) ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Caption(label)
                            Text(value, color = Brand.TextPrimary, fontSize = 15.sp)
                        }
                    }
                    if (triggers.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Caption("When to use")
                            triggers.forEach { Bullet(it) }
                        }
                    }
                }
            }
        }

        if (sop.steps.isNotEmpty()) {
            SectionTitle("Steps")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sop.steps.forEachIndexed { i, step -> StepCard(i + 1, step, ::open) }
            }
        }

        val refs = sop.references.orEmpty()
        if (refs.isNotEmpty()) {
            SectionTitle("References")
            Card(padding = 0) {
                refs.forEachIndexed { i, r ->
                    if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 14.dp).height(1.dp).background(Brand.TextPrimary.copy(alpha = 0.08f)))
                    ReferenceRow(r, ::open)
                }
            }
        }

        sop.source?.takeIf { it.isNotBlank() }?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Description, contentDescription = null, tint = Brand.TextSecondary, modifier = Modifier.size(14.dp))
                Text("Source: $it", color = Brand.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun StepCard(n: Int, step: SopStep, open: (String) -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(Brand.Premier),
                contentAlignment = Alignment.Center,
            ) { Text("$n", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(step.title, color = Brand.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (!step.owner.isNullOrBlank() || !step.timing.isNullOrBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        step.owner?.takeIf { it.isNotBlank() }?.let { Chip(Icons.Filled.Person, it) }
                        step.timing?.takeIf { it.isNotBlank() }?.let { Chip(Icons.Filled.Schedule, it) }
                    }
                }
                if (step.detail.isNotBlank()) Text(step.detail, color = Brand.TextPrimary.copy(alpha = 0.85f), fontSize = 14.sp)
                step.items.orEmpty().filter { it.isNotBlank() }.forEach { Bullet(it) }
                step.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Brand.Amber.copy(alpha = 0.12f)).padding(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Brand.Amber, modifier = Modifier.size(16.dp))
                        Text(note, color = Brand.Amber, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                }
                step.links.orEmpty().filter { SopRules.linkUrl(it.href) != null }.forEach { l ->
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { open(l.href) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(14.dp))
                        Text(l.label.ifBlank { l.href }, color = Brand.Accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceRow(r: SopReference, open: (String) -> Unit) {
    val href = r.href?.takeIf { SopRules.linkUrl(it) != null }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (href != null) Modifier.clickable { open(href) } else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(r.label, color = Brand.TextSecondary, fontSize = 12.sp)
        Text(r.value, color = if (href != null) Brand.Accent else Brand.TextPrimary, fontSize = 15.sp)
    }
}

@Composable
private fun Chip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Brand.Accent, modifier = Modifier.size(14.dp))
        Text(text, color = Brand.Accent, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun Caption(text: String) {
    Text(text.uppercase(), color = Brand.TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 8.dp).size(5.dp).clip(CircleShape).background(Brand.Accent))
        Text(text, color = Brand.TextPrimary.copy(alpha = 0.85f), fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
    }
}
