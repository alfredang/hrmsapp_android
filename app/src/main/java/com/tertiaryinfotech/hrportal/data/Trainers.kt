package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

// Trainer directory for Trainer Admins — `GET /api/mobile/trainers` and `/api/mobile/trainers/{id}`.
// Same handler and payload as the web's `/api/trainers` (src/lib/trainers.ts, prisma `Trainer`).
// The detail's courses / recentClasses / calendarClasses are free-form JSON columns filled by the
// importer, so every field is defaulted and number-or-string tolerant.

@Serializable
data class TrainersResponse(
    val count: Int = 0,
    val trainers: List<TrainerListItem> = emptyList(),
)

/** One directory row. No NRIC in the list, by design. */
@Serializable
data class TrainerListItem(
    val id: String,
    val name: String = "",
    val nickname: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val trainerType: String? = null,
    val status: String = "",
    val inTms: Boolean = false,
    val trainingCerts: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val education: String? = null,
    val summary: String? = null,
    val financeBackground: Boolean = false,
    val ibfFitDeclaration: Boolean = false,
    val classesCount: Int = 0,
    val lastClassDate: String? = null,
    val courseTitles: List<String> = emptyList(),
) {
    /** Professional certs first, then skills — what the row previews under the name. */
    fun topTags(limit: Int = 4): List<String> = (certifications + skills).distinct().take(limit)
}

@Serializable
data class TrainerDetailResponse(val trainer: TrainerProfile)

/** The full profile, including the NRIC — always shown masked until the user taps Show. */
@Serializable
data class TrainerProfile(
    val id: String = "",
    val name: String = "",
    val nickname: String? = null,
    val nric: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val secondaryEmail: String? = null,
    val nationality: String? = null,
    val linkedinUrl: String? = null,
    val trainerType: String? = null,
    val status: String = "",
    val inTms: Boolean = false,
    val trainingCerts: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val education: String? = null,
    val experience: List<String> = emptyList(),
    val summary: String? = null,
    val financeBackground: Boolean = false,
    val financeEvidence: List<String> = emptyList(),
    val ibfFitDeclaration: Boolean = false,
    val ibfFitFiles: List<String> = emptyList(),
    val actaFolder: String? = null,
    val evidenceFiles: List<String> = emptyList(),
    val courses: List<TrainerCourse>? = null,
    val recentClasses: List<TrainerClass>? = null,
    val calendarClasses: List<TrainerCalendarClass>? = null,
    val classesCount: Int = 0,
    val lastClassDate: String? = null,
    val adminNotes: String? = null,
)

@Serializable
data class TrainerCourse(
    @Serializable(with = FlexStringSerializer::class) val title: String = "",
    @Serializable(with = FlexStringSerializer::class) val code: String = "",
    @Serializable(with = FlexNumberSerializer::class) val classes: Double = 0.0,
    @Serializable(with = FlexStringSerializer::class) val first: String = "",
    @Serializable(with = FlexStringSerializer::class) val last: String = "",
)

@Serializable
data class TrainerClass(
    @Serializable(with = FlexStringSerializer::class) val start: String = "",
    @Serializable(with = FlexStringSerializer::class) val title: String = "",
    @Serializable(with = FlexStringSerializer::class) val run: String = "",
    @Serializable(with = FlexStringSerializer::class) val status: String = "",
)

@Serializable
data class TrainerCalendarClass(
    @Serializable(with = FlexStringSerializer::class) val date: String = "",
    @Serializable(with = FlexStringSerializer::class) val title: String = "",
    @Serializable(with = FlexStringSerializer::class) val calendar: String = "",
)

/** Accepts a JSON string, number or boolean (or null) and returns it as text. */
object FlexStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonNull) return ""
        return (element as? JsonPrimitive)?.content ?: element.toString()
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

/** Directory filters, sent as query parameters (the server does the matching). */
data class TrainerQuery(
    val q: String = "",
    val certs: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val finance: Boolean = false,
    val ibfFit: Boolean = false,
    val status: TrainerStatusFilter = TrainerStatusFilter.TRAINERS,
)

enum class TrainerStatusFilter(val param: String, val label: String) {
    TRAINERS("trainers", "Trainers"),
    APPLICANTS("applicants", "Applicants"),
    ALL("all", "All"),
}

object TrainerFormat {
    /** The accreditation filters offered as chips (web `TRAINING_CERTS`). */
    val TRAINING_CERTS = listOf("ACLP", "ACTA", "DACE", "DDDLP")

    /** First letter + **** + last 4, as the web's `maskNric`. Short values mask fully. */
    fun maskNric(nric: String?): String {
        if (nric.isNullOrBlank()) return ""
        val n = nric.trim()
        return if (n.length >= 6) "${n.first()}****${n.takeLast(4)}" else "****"
    }

    /** `tel:` with spaces and dashes stripped, keeping a leading +. */
    fun telUri(phone: String): String = "tel:" + phone.filter { it.isDigit() || it == '+' }

    /** A LinkedIn value may be a bare handle path; give it a scheme so it opens. */
    fun webUrl(url: String): String =
        if (url.startsWith("http://") || url.startsWith("https://")) url else "https://${url.trimStart('/')}"
}

/** The single-choice Accreditation filter (web "Accreditation" dropdown). */
enum class Accreditation(val label: String) {
    ANY("Any accreditation"),
    ACLP("ACLP"),
    ACTA("ACTA"),
    DACE("DACE"),
    DDDLP("DDDLP"),
    FINANCE("Finance background"),
    IBF("IBF Fit & Proper"),
    ;

    /** Short label for the filter button and the removable chip. */
    val short: String get() = when (this) {
        FINANCE -> "Finance"
        IBF -> "IBF Fit"
        else -> label
    }

    fun applyTo(q: TrainerQuery): TrainerQuery = when (this) {
        ANY -> q.copy(certs = emptySet(), finance = false, ibfFit = false)
        FINANCE -> q.copy(certs = emptySet(), finance = true, ibfFit = false)
        IBF -> q.copy(certs = emptySet(), finance = false, ibfFit = true)
        else -> q.copy(certs = setOf(name), finance = false, ibfFit = false)
    }

    fun count(pool: List<TrainerListItem>): Int = when (this) {
        ANY -> pool.size
        FINANCE -> pool.count { it.financeBackground }
        IBF -> pool.count { it.ibfFitDeclaration }
        else -> pool.count { name in it.trainingCerts }
    }
}

/** A skill or professional certification with the number of trainers who have it. */
data class TagCount(val tag: String, val count: Int, val kind: Kind) {
    enum class Kind(val label: String) { CERTIFICATION("Certification"), SKILL("Skill") }
}

/**
 * Client-side directory search — a 1:1 port of the web's `src/lib/trainers.ts` (`haystack`,
 * `matchesQuery`, `filterTrainers`, `topTags`) and the suggestion list in `trainer-directory.tsx`.
 * The app loads the whole directory once and filters on the device, as the web page does, so the
 * suggestions can show counts and results update as you type.
 */
object TrainerSearch {
    private val SPACES = Regex("\\s+")
    private fun norm(s: String) = s.lowercase().replace(SPACES, " ").trim()

    /** Common alternative spellings people search for. */
    private val ALIASES = mapOf("dddpl" to "dddlp", "alcp" to "aclp", "ccar f" to "ccar-f", "ccarf" to "ccar-f")

    fun isApplicant(t: TrainerListItem): Boolean = t.status == "Applicant" || t.status == "Contacted"

    fun haystack(t: TrainerListItem): String = norm(
        (
            listOf(t.name, t.nickname.orEmpty(), t.email.orEmpty(), t.phone.orEmpty(), t.trainerType.orEmpty(),
                t.education.orEmpty(), t.summary.orEmpty()) +
                t.trainingCerts + t.certifications + t.skills + t.courseTitles +
                listOf(if (t.financeBackground) "finance" else "", if (t.ibfFitDeclaration) "ibf fit" else "")
            ).joinToString(" | "),
    )

    /** Every whitespace-separated term must appear somewhere in the trainer's profile. */
    fun matchesQuery(hay: String, q: String?): Boolean {
        if (q.isNullOrBlank()) return true
        return norm(q).split(" ").filter { it.isNotEmpty() }.all { term -> hay.contains(ALIASES[term] ?: term) }
    }

    private fun hasTag(t: TrainerListItem, tag: String): Boolean {
        val n = norm(tag)
        return (t.skills + t.certifications + t.trainingCerts).any { norm(it) == n }
    }

    fun filter(list: List<TrainerListItem>, f: TrainerQuery): List<TrainerListItem> = list.filter { t ->
        when {
            f.status == TrainerStatusFilter.TRAINERS && isApplicant(t) -> false
            f.status == TrainerStatusFilter.APPLICANTS && !isApplicant(t) -> false
            f.finance && !t.financeBackground -> false
            f.ibfFit && !t.ibfFitDeclaration -> false
            f.certs.isNotEmpty() && !f.certs.all { it in t.trainingCerts } -> false
            f.tags.isNotEmpty() && !f.tags.all { hasTag(t, it) } -> false
            else -> matchesQuery(haystack(t), f.q)
        }
    }

    /** Certifications first, then skills, each by trainer count; case-insensitive duplicates dropped. */
    fun vocabulary(pool: List<TrainerListItem>): List<TagCount> {
        fun count(field: (TrainerListItem) -> List<String>, kind: TagCount.Kind): List<TagCount> {
            val counts = LinkedHashMap<String, Pair<String, Int>>()
            for (t in pool) {
                val seen = HashSet<String>()
                for (raw in field(t)) {
                    val key = norm(raw)
                    if (key.isEmpty() || !seen.add(key)) continue
                    val cur = counts[key]
                    counts[key] = if (cur == null) raw.trim() to 1 else cur.first to cur.second + 1
                }
            }
            return counts.values.map { (tag, n) -> TagCount(tag, n, kind) }
                .sortedWith(compareByDescending<TagCount> { it.count }.thenBy { it.tag.lowercase() })
        }
        val seen = HashSet<String>()
        return (count({ it.certifications }, TagCount.Kind.CERTIFICATION) + count({ it.skills }, TagCount.Kind.SKILL))
            .filter { seen.add(it.tag.lowercase()) }
    }

    /** Up to [limit] suggestions for the typed text: starts-with first, then contains; popular picks when empty. */
    fun suggestions(vocab: List<TagCount>, typed: String, selected: Collection<String>, limit: Int = 8): List<TagCount> {
        val taken = selected.map { it.lowercase() }.toSet()
        val pool = vocab.filter { it.tag.lowercase() !in taken }
        val needle = typed.trim().lowercase()
        if (needle.isEmpty()) return pool.take(limit)
        val starts = pool.filter { it.tag.lowercase().startsWith(needle) }
        val contains = pool.filter { !it.tag.lowercase().startsWith(needle) && it.tag.lowercase().contains(needle) }
        return (starts + contains).take(limit)
    }

    /** Expertise with the items matching the active search / tags first, so the reason a trainer matched shows. */
    fun orderByMatch(items: List<String>, needles: List<String>): List<String> {
        val ns = needles.map { it.trim().lowercase() }.filter { it.length > 1 }
        if (ns.isEmpty()) return items
        val (hit, rest) = items.partition { s -> ns.any { s.lowercase().contains(it) } }
        return hit + rest
    }

    /** `https://wa.me/<digits>`; an 8-digit number is a Singapore number stored without +65. */
    fun whatsAppUrl(phone: String?): String? {
        val digits = phone.orEmpty().filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return "https://wa.me/" + if (digits.length == 8) "65$digits" else digits
    }
}


// MARK: - Trainer contact list (GET /api/mobile/trainers/contacts — every signed-in user)

@Serializable
data class TrainerContactsResponse(
    val count: Int = 0,
    val trainers: List<TrainerContact> = emptyList(),
)

@Serializable
data class TrainerContact(
    val id: String,
    val name: String = "",
    val email: String? = null,
    val phone: String? = null,
    val trainingCerts: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val courseTitles: List<String> = emptyList(),
    val lastClassDate: String? = null,
) {
    /** One line under the name: courses first, then skills. */
    val expertiseLine: String get() = (courseTitles + skills).filter { it.isNotBlank() }.distinct().joinToString(" · ")
}

/** Last list loaded, so the detail screen opens instantly without a second request. */
object TrainerContactsCache {
    @Volatile var byId: Map<String, TrainerContact> = emptyMap()
}

/**
 * Short chip label for a training certificate. Trainer records sometimes hold the full title
 * ("Advanced Certificate in Learning and Performance (ACLP, 2023)"); chips show only the code
 * (ACLP / ACTA / DACE / DDDLP, with the common DDDPL / ALCP typos fixed), else ~14 characters.
 */
object CertLabel {
    private val codes = listOf("DDDLP" to "DDDLP", "DDDPL" to "DDDLP", "ACLP" to "ACLP", "ALCP" to "ACLP", "ACTA" to "ACTA", "DACE" to "DACE")
    fun short(cert: String): String {
        val up = cert.uppercase()
        codes.firstOrNull { (k, _) -> Regex("\\b$k\\b").containsMatchIn(up) }?.let { return it.second }
        val t = cert.trim()
        return if (t.length <= 14) t else t.take(13).trimEnd() + "…"
    }
    fun shortAll(certs: List<String>): List<String> = certs.map(::short).filter { it.isNotBlank() }.distinct()
}
