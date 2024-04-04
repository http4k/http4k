package org.http4k.lens

import org.http4k.base64Decoded
import org.http4k.base64Encode
import org.http4k.core.Credentials
import org.http4k.events.EventCategory
import org.http4k.filter.SamplingDecision
import org.http4k.filter.TraceId

fun StringBiDiMappings.eventCategory() = BiDiMapping(::EventCategory, EventCategory::toString)
fun StringBiDiMappings.traceId() = BiDiMapping(::TraceId, TraceId::value)
fun StringBiDiMappings.samplingDecision() = BiDiMapping(::SamplingDecision, SamplingDecision::value)

fun <IN : Any> BiDiLensSpec<IN, String>.basicCredentials() = map(StringBiDiMappings.basicCredentials())

fun StringBiDiMappings.basicCredentials() = BiDiMapping(
    { value ->
        value.trim()
            .takeIf { value.startsWith("Basic") }
            ?.substringAfter("Basic")
            ?.trim()
            ?.safeBase64Decoded()
            ?.split(":", ignoreCase = false, limit = 2)
            .let { Credentials(it?.getOrNull(0) ?: "", it?.getOrNull(1) ?: "") }
    },
    { credentials: Credentials -> "Basic ${"${credentials.user}:${credentials.password}".base64Encode()}" }
)

private fun String.safeBase64Decoded(): String? = try {
    base64Decoded()
} catch (e: IllegalArgumentException) {
    null
}
