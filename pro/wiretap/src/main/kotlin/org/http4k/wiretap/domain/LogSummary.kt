/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.logs.data.LogRecordData
import org.http4k.wiretap.util.Json
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter.ofPattern

data class LogSummary(
    val timestamp: String,
    val severity: String,
    val body: String,
    val serviceName: String,
    val traceId: String?,
    val spanId: String?,
    val attributes: List<SpanAttribute>,
    val bodyFields: List<SpanAttribute>
)

fun LogRecordData.toSummary(clock: Clock) = LogSummary(
    timestamp = ofPattern("HH:mm:ss.SSS").withZone(clock.zone)
        .format(Instant.ofEpochSecond(0, this.timestampEpochNanos)),
    severity = severity.name,
    body = this.bodyValue?.asString() ?: "",
    serviceName = resource.attributes.get(AttributeKey.stringKey("service.name")) ?: "",
    traceId = spanContext.traceId.takeIf { it != "00000000000000000000000000000000" },
    spanId = spanContext.spanId.takeIf { it != "0000000000000000" },
    attributes = attributes.asMap().map { (k, v) -> SpanAttribute(k.key, v.toString()) },
    bodyFields = parseJsonFields(this.bodyValue?.asString() ?: "")
)

private fun parseJsonFields(body: String) =
    when {
        body.isBlank() -> emptyList()
        else -> try {
            Json.fields(Json.parse(body)).map { SpanAttribute(it.first, Json.text(it.second)) }
        } catch (_: Exception) {
            listOf(SpanAttribute("body", body))
        }
    }
