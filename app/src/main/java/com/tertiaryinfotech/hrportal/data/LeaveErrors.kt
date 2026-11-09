package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.Serializable

/**
 * No Pay Leave cap status as the server reports it (`getNplStatus` in the web app): the cap is
 * 21 days a year prorated by completed months served, and [remaining] = cap - used - pending.
 */
@Serializable
data class NplStatus(
    @Serializable(with = FlexNumberSerializer::class) val cap: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val used: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val pending: Double = 0.0,
    @Serializable(with = FlexNumberSerializer::class) val remaining: Double = 0.0,
)

/** The `POST /api/leave` error body. Every field is optional: older errors only carry `error`. */
@Serializable
data class LeaveErrorBody(
    val error: String? = null,
    val code: String? = null,
    val alAvailable: Double? = null,
    val excessDays: Double? = null,
    val npl: NplStatus? = null,
    val suggestedLeaveTypeCode: String? = null,
    val requested: Double? = null,
)

/** Why the server refused a leave request, with what the form needs to offer a way forward. */
sealed interface LeaveRejection {
    val message: String

    /** AL beyond the balance (+ advance + Off In Lieu): the rest must be No Pay Leave. */
    data class AlExceedsBalance(
        override val message: String,
        val alAvailable: Double,
        val excessDays: Double,
        val npl: NplStatus?,
        val suggestedLeaveTypeCode: String,
    ) : LeaveRejection

    /** Interns have no paid MC: their sick leave is No Pay Leave. */
    data class InternMcNotPaid(
        override val message: String,
        val suggestedLeaveTypeCode: String,
    ) : LeaveRejection

    /** The request is more than the No Pay Leave left this year. */
    data class NplCapExceeded(
        override val message: String,
        val npl: NplStatus?,
        val requested: Double?,
    ) : LeaveRejection
}

/** Thrown by [HrmsApi.applyLeave] for the coded refusals above; anything else stays an [ApiException]. */
class LeaveRejectedException(val rejection: LeaveRejection) : Exception(rejection.message)

object LeaveErrors {
    const val AL_EXCEEDS_BALANCE = "AL_EXCEEDS_BALANCE"
    const val INTERN_MC_NOT_PAID = "INTERN_MC_NOT_PAID"
    const val NPL_CAP_EXCEEDED = "NPL_CAP_EXCEEDED"
    const val NPL_CODE = "NPL"

    /** Parses a `POST /api/leave` error body. Null when it isn't JSON or carries no known `code`. */
    fun parse(body: String?): LeaveRejection? {
        if (body.isNullOrBlank()) return null
        val e = try {
            HrmsJson.decodeFromString(LeaveErrorBody.serializer(), body)
        } catch (_: Exception) {
            return null
        }
        val suggested = e.suggestedLeaveTypeCode?.takeIf { it.isNotBlank() } ?: NPL_CODE
        return when (e.code) {
            AL_EXCEEDS_BALANCE -> LeaveRejection.AlExceedsBalance(
                message = e.error ?: "This is more annual leave than you have available. Apply for the rest as No Pay Leave.",
                alAvailable = e.alAvailable ?: 0.0,
                excessDays = e.excessDays ?: 0.0,
                npl = e.npl,
                suggestedLeaveTypeCode = suggested,
            )
            INTERN_MC_NOT_PAID -> LeaveRejection.InternMcNotPaid(
                message = e.error ?: "Interns do not have paid medical leave. Apply for sick leave as No Pay Leave.",
                suggestedLeaveTypeCode = suggested,
            )
            NPL_CAP_EXCEEDED -> LeaveRejection.NplCapExceeded(
                message = e.error ?: "This is more No Pay Leave than you have left this year.",
                npl = e.npl,
                requested = e.requested,
            )
            else -> null
        }
    }
}

/** Pure leave rules the form applies before it talks to the server (mirrors `leave-policy.ts`). */
object LeaveRules {
    val MEDICAL_CODES = setOf("MC", "SL", "MEDICAL")
    const val INTERN_SICK_REASON = "Sick leave (MC)"

    /** An intern is a user whose roles are exactly ["INTERN"]; admins also hold INTERN to switch views. */
    fun isIntern(roles: List<String>?, role: String? = null): Boolean =
        if (roles != null) roles.size == 1 && roles[0] == "INTERN" else role == "INTERN"

    fun isMedical(code: String?): Boolean = code != null && code.uppercase() in MEDICAL_CODES

    /** The reason an intern's sick leave carries once it goes in as No Pay Leave (as the web form does). */
    fun internSickReason(reason: String): String {
        val r = reason.trim()
        return when {
            r.isEmpty() || r == INTERN_SICK_REASON -> INTERN_SICK_REASON
            r.startsWith(INTERN_SICK_REASON) -> r
            else -> "$INTERN_SICK_REASON — $r"
        }
    }

    /** The id of the leave type with [code] (e.g. "NPL") among the types the app already loaded. */
    fun typeIdFor(types: List<LeaveType>, code: String): String? =
        types.firstOrNull { it.code.equals(code, ignoreCase = true) }?.id

    /** The No Pay Leave cap and remaining days from the `/api/mobile/leave` balances (proRated = cap). */
    fun nplFromBalances(balances: List<LeaveBalance>): NplStatus? {
        val b = balances.firstOrNull { it.code == NPL_CODE_ROW } ?: return null
        val cap = b.proRated ?: return null
        return NplStatus(cap = cap, used = b.used, pending = b.pending, remaining = b.available)
    }

    private const val NPL_CODE_ROW = "NPL"
}
