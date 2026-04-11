/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import org.http4k.core.Uri
import org.http4k.filter.LegacyHttp4kConventions
import org.http4k.filter.OpenTelemetrySemanticConventions

data class TraceSummary(
    val traceId: OtelTraceId,
    val spanCount: Int,
    val rootSpanName: String,
    val serviceName: String,
    val totalDurationMs: Long,
    val timestamp: String
)

data class TraceDetail(
    val traceId: OtelTraceId,
    val totalDurationMs: Long,
    val spans: List<SpanDetail>
)

data class SpanDetail(
    val spanId: OtelSpanId,
    val parentSpanId: OtelSpanId,
    val name: String,
    val kind: String,
    val durationMs: Long,
    val statusCode: String,
    val statusDescription: String,
    val serviceName: String,
    val depth: Int,
    val startOffsetPercent: Double,
    val widthPercent: Double,
    val attributes: List<SpanAttribute>,
    val baggageAttributes: List<SpanAttribute>,
    val resourceAttributes: List<SpanAttribute>,
    val events: List<SpanEvent>,
    val links: List<SpanLink>
)

data class SpanAttribute(val key: String, val value: String)

data class SpanEvent(
    val name: String,
    val timestampMs: Long,
    val attributes: List<SpanAttribute>
)

data class SpanLink(
    val traceId: OtelTraceId,
    val spanId: OtelSpanId,
    val attributes: List<SpanAttribute>
)

fun SpanDetail.httpStatusCode(): Int? =
    attributes.firstOrNull {
        it.key == OpenTelemetrySemanticConventions.statusCode ||
            it.key == LegacyHttp4kConventions.statusCode
    }?.value?.toIntOrNull()

fun SpanDetail.isError(): Boolean = httpStatusCode()?.let { it >= 500 } ?: (statusCode == "ERROR")

fun SpanDetail.remoteAuthority(): String = attributes
    .firstOrNull {
        it.key == OpenTelemetrySemanticConventions.clientUrl ||
            it.key == LegacyHttp4kConventions.clientUrl
    }
    ?.value
    ?.let { Uri.of(it) }
    ?.authority
    ?.ifEmpty { null }
    ?: "unknown"
