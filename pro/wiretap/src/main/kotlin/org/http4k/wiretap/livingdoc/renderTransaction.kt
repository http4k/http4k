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

private val filteredHeaders = setOf("content-length", "traceparent", "tracestate")

fun renderTransaction(wtx: WiretapTransaction): String {
    val req = wtx.transaction.request
    val resp = wtx.transaction.response
    val host = req.header("Host") ?: req.uri.authority
    val label = "${req.method} ${req.uri.path}" + if (host.isEmpty()) "" else " → $host"

    return buildString {
        appendLine()
        appendLine("#### $label")
        appendLine()
        appendHttpBlock("Request", req)
        appendLine()
        appendHttpBlock("Response", resp)
    }
}

private fun StringBuilder.appendHttpBlock(label: String, message: HttpMessage) {
    val headerLine = when (message) {
        is Request -> "${message.method} ${message.uri} HTTP/1.1"
        is Response -> "HTTP/1.1 ${message.status}"
        else -> ""
    }
    appendLine("**$label** `$headerLine`")

    val headers = message.headers.filter { it.first.lowercase() !in filteredHeaders }
    if (headers.isNotEmpty()) {
        appendLine()
        appendLine("| Header | Value |")
        appendLine("|---|---|")
        headers.forEach { (name, value) -> appendLine("| $name | $value |") }
    }

    val body = message.bodyString()
    val contentType = message.header("Content-Type") ?: ""
    if (body.isNotBlank()) {
        val lang = when {
            contentType.contains("json") -> "json"
            contentType.contains("xml") -> "xml"
            contentType.contains("html") -> "html"
            else -> ""
        }
        appendLine()
        appendLine("<details><summary>Body</summary>")
        appendLine()
        appendLine("```$lang")
        appendLine(formatBody(body, contentType))
        appendLine("```")
        appendLine()
        appendLine("</details>")
    }
}
