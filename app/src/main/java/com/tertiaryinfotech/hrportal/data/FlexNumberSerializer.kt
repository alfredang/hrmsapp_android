package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * Decodes a Double that may arrive either as a JSON number (`8.0`) or as a numeric
 * string (`"8.00"`) — the existing /api/timesheet endpoint returns decimal strings.
 * Mirrors `TimesheetDay.flexNumber` in the iOS model.
 */
object FlexNumberSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexNumber", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            val primitive = element as? JsonPrimitive ?: return 0.0
            return primitive.doubleOrNull ?: primitive.content.toDoubleOrNull() ?: 0.0
        }
        return try {
            decoder.decodeDouble()
        } catch (_: Exception) {
            0.0
        }
    }

    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
}
