/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.core.Uri
import org.http4k.wiretap.domain.OtelSpanId
import org.http4k.wiretap.domain.Participant
import org.http4k.wiretap.domain.SequenceDiagram
import org.http4k.wiretap.domain.SequenceMessage
import org.http4k.wiretap.domain.SpanDetail
import org.http4k.wiretap.domain.TraceDetail

fun TraceDetail.toSequenceDiagram(): SequenceDiagram {
    if (spans.isEmpty()) return SequenceDiagram(emptyList(), emptyList())

    val spanById = spans.associateBy { it.spanId }

    val rootServerSpan = spans.find {
        it.kind == "SERVER" && it.serviceName.isNotEmpty() &&
            (it.parentSpanId == ROOT_PARENT_SPAN_ID || it.parentSpanId !in spanById)
    }

    val serviceOrder = mutableListOf<String>()

    if (rootServerSpan != null) {
        serviceOrder.add("wiretap")
    }

    spans.forEach { span ->
        if (span.serviceName.isNotEmpty() && span.serviceName !in serviceOrder) {
            serviceOrder.add(span.serviceName)
        }
    }

    val serviceIndex = serviceOrder.withIndex().associate { (i, name) -> name to i }.toMutableMap()

    fun indexFor(name: String): Int = serviceIndex.getOrPut(name) {
        serviceOrder.add(name)
        serviceOrder.size - 1
    }

    val messages = mutableListOf<SequenceMessage>()

    if (rootServerSpan != null) {
        messages.add(
            SequenceMessage(
                spanId = rootServerSpan.spanId,
                fromIndex = 0,
                toIndex = indexFor(rootServerSpan.serviceName),
                label = rootServerSpan.name,
                isResponse = false,
                isError = false
            )
        )

        spans.filter { it.kind == "CLIENT" }.forEach { clientSpan ->
            addClientSpanMessages(clientSpan, ::indexFor, messages, spanById)
        }

        messages.add(
            SequenceMessage(
                spanId = rootServerSpan.spanId,
                fromIndex = indexFor(rootServerSpan.serviceName),
                toIndex = 0,
                label = rootServerSpan.responseLabel(),
                isResponse = true,
                isError = rootServerSpan.isError()
            )
        )
    } else {
        spans.filter { it.kind == "CLIENT" }.forEach { clientSpan ->
            addClientSpanMessages(clientSpan, ::indexFor, messages, spanById)
        }
    }

    return SequenceDiagram(serviceOrder.mapIndexed { i, name -> Participant(name, i) }, messages)
}

private fun addClientSpanMessages(
    clientSpan: SpanDetail,
    indexFor: (String) -> Int,
    messages: MutableList<SequenceMessage>,
    spanById: Map<OtelSpanId, SpanDetail>
) {
    val fromIdx = indexFor(clientSpan.serviceName)

    val childServer = spanById.values.find {
        it.parentSpanId == clientSpan.spanId && it.kind == "SERVER"
    }

    val toIdx = when {
        childServer != null -> indexFor(childServer.serviceName)
        else -> clientSpan.remoteAuthority()?.let { indexFor(it) } ?: fromIdx
    }

    messages.add(
        SequenceMessage(clientSpan.spanId, fromIdx, toIdx, clientSpan.clientLabel(), isResponse = false, isError = false)
    )
    messages.add(
        SequenceMessage(
            clientSpan.spanId, toIdx, fromIdx, clientSpan.responseLabel(), true, clientSpan.isError()
        )
    )
}

private fun SpanDetail.clientLabel(): String {
    val url = attributes
        .firstOrNull { it.key == "url.full" || it.key == "http.url" }
        ?.value
        ?.let { Uri.of(it) }
    val path = url?.path?.ifEmpty { null } ?: return name
    val method = name.split(" ").firstOrNull() ?: return name
    return "$method $path"
}

private fun SpanDetail.responseLabel() = "${httpStatusCode()?.toString() ?: statusCode} (${durationMs}ms)"

private fun SpanDetail.isError() = httpStatusCode()?.let { it >= 500 } ?: (statusCode == "ERROR")

private fun SpanDetail.httpStatusCode(): Int? =
    attributes.firstOrNull { it.key == "http.response.status_code" || it.key == "http.status_code" }
        ?.value?.toIntOrNull()

private fun SpanDetail.remoteAuthority() = attributes
    .firstOrNull { it.key == "url.full" || it.key == "http.url" }
    ?.value
    ?.let { Uri.of(it) }
    ?.authority
    ?.ifEmpty { null }
    ?: "unknown"

private val ROOT_PARENT_SPAN_ID = OtelSpanId.of("0000000000000000")

fun SequenceDiagram.toMermaid(): String {
    if (participants.isEmpty()) return ""

    val alias = { p: Participant -> "P${p.index}" }
    val needsAlias = { name: String -> name.any { it in ":{}\"';<>" || it.isWhitespace() } }

    val lines = mutableListOf("sequenceDiagram")

    lines.addAll(participants.map { p ->
        when {
            needsAlias(p.serviceName) -> "    participant ${alias(p)} as ${p.serviceName}"
            else -> "    participant ${p.serviceName}"
        }
    })

    lines.addAll(messages.map { msg ->
        val from = participants[msg.fromIndex].let { if (needsAlias(it.serviceName)) alias(it) else it.serviceName }
        val to = participants[msg.toIndex].let { if (needsAlias(it.serviceName)) alias(it) else it.serviceName }
        val arrow = when {
            msg.isError && msg.isResponse -> "--x"
            msg.isError -> "-x"
            msg.isResponse -> "-->>"
            else -> "->>"
        }
        "    $from${arrow}$to: ${msg.label}"
    })

    return lines.joinToString("\n")
}
