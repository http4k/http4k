/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.wiretap.domain.OtelSpanId
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.remoteAuthority

private val ROOT_PARENT_SPAN_ID = OtelSpanId.of("0000000000000000")

fun TraceDetail.toInteractionDiagram(): String {
    if (spans.isEmpty()) return ""

    val spanById = spans.associateBy { it.spanId }

    val rootServerSpan = spans.find {
        it.kind == "SERVER" && it.serviceName.isNotEmpty() &&
            (it.parentSpanId == ROOT_PARENT_SPAN_ID || it.parentSpanId !in spanById)
    }

    val clientRelationships = spans.filter { it.kind == "CLIENT" }.map { clientSpan ->
        val from = clientSpan.serviceName
        val childServer = spanById.values.find {
            it.parentSpanId == clientSpan.spanId && it.kind == "SERVER"
        }
        val to = childServer?.serviceName ?: clientSpan.remoteAuthority()
        from to to
    }

    val rootRelationship = rootServerSpan?.let { listOf("wiretap" to it.serviceName) } ?: emptyList()

    val relationships = (rootRelationship + clientRelationships).toSet()
    if (relationships.isEmpty()) return ""

    val services = relationships.flatMap { (from, to) -> listOf(from, to) }.distinct()

    val lines = mutableListOf("C4Context")
    lines.add("    UpdateLayoutConfig(\$c4ShapeInRow=\"1\", \$c4BoundaryInRow=\"1\")")
    lines.addAll(services.map { "    System(${it.identifier()}, \"$it\")" })
    lines.addAll(relationships.map { (from, to) -> "    Rel_D(${from.identifier()}, ${to.identifier()}, \" \")" })

    return lines.joinToString("\n")
}

private fun String.identifier() = filter { it.isLetterOrDigit() }
