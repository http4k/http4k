/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.livingdoc

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.wiretap.domain.WiretapTransaction
import org.http4k.wiretap.util.formatBody

fun renderTransaction(wtx: WiretapTransaction): String {
    val req = wtx.transaction.request
    val resp = wtx.transaction.response
    val host = req.header("Host") ?: req.uri.authority
    val label = "${req.method} ${req.uri.path}" + if (host.isNullOrEmpty()) "" else " → $host"

    return buildString {
        appendLine()
        appendLine("#### $label")
        appendLine()
        appendLine("**Request**")
        appendLine()
        append(renderHttpBlock(req))
        appendLine()
        appendLine("**Response**")
        appendLine()
        append(renderHttpBlock(resp))
    }
}

private fun renderHttpBlock(message: HttpMessage) = buildString {
    appendLine("```http")
    when (message) {
        is Request -> appendLine("${message.method} ${message.uri} HTTP/1.1")
        is Response -> appendLine("HTTP/1.1 ${message.status}")
    }
    message.headers
        .filter { it.first.lowercase() !in setOf("content-length", "traceparent", "tracestate") }
        .forEach { (name, value) -> appendLine("$name: $value") }
    val body = message.bodyString()
    val contentType = message.header("Content-Type") ?: ""
    if (body.isNotBlank()) {
        appendLine()
        appendLine(formatBody(body, contentType))
    }
    appendLine("```")
}
