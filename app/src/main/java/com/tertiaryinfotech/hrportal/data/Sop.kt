package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.Serializable

/**
 * The company's Standard Operating Procedures (`GET /api/mobile/sops`, every signed-in employee; web /sop), shown
 * natively so no browser sign-in is needed (Alfred, 22 Nov 2026; iOS `Models/SopModels.swift`). Decoded leniently:
 * every field but [Sop.slug] has a default.
 */
@Serializable
data class SopsResponse(val sops: List<Sop> = emptyList())

@Serializable
data class Sop(
    val slug: String,
    val title: String = slug,
    val category: String = "Other",
    val summary: String = "",
    val purpose: String? = null,
    val scope: String? = null,
    val owner: String? = null,
    val triggers: List<String>? = null,
    val steps: List<SopStep> = emptyList(),
    val references: List<SopReference>? = null,
    val source: String? = null,
    val updatedAt: String? = null,
) {
    /** Every word of the query must appear in the title, summary, category, owner or a step (as the web). */
    fun matches(query: String): Boolean {
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return true
        val hay = (
            listOf(title, summary, category, owner.orEmpty(), purpose.orEmpty(), scope.orEmpty()) + triggers.orEmpty() +
                steps.flatMap { listOf(it.title, it.detail, it.owner.orEmpty(), it.timing.orEmpty(), it.note.orEmpty()) + it.items.orEmpty() }
            ).joinToString(" ").lowercase()
        return terms.all { hay.contains(it) }
    }

    /** "3 steps · HR". */
    val stepsLine: String
        get() = "${steps.size} step${if (steps.size == 1) "" else "s"}" + (owner?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
}

@Serializable
data class SopStep(
    val title: String = "",
    val detail: String = "",
    val items: List<String>? = null,
    val owner: String? = null,
    val timing: String? = null,
    val note: String? = null,
    val links: List<SopLink>? = null,
)

@Serializable
data class SopLink(val label: String = "", val href: String = "")

@Serializable
data class SopReference(val label: String = "", val value: String = "", val href: String? = null)

object SopRules {
    /** Categories in the order the server lists the SOPs (the web's sort order). */
    fun orderedCategories(list: List<Sop>): List<String> = list.map { it.category }.distinct()

    /** Absolute links as given; a site path ("/leave") opens on the HRMS website. */
    fun linkUrl(href: String): String? {
        val h = href.trim()
        if (h.isEmpty()) return null
        return if (h.startsWith("/")) Net.BASE_URL.trimEnd('/') + h else h
    }
}

/** The SOP list loaded last, so a detail screen opens at once without a second request. */
object SopCache {
    @Volatile var all: List<Sop> = emptyList()
}
