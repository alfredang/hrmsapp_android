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
