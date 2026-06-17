/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.extractor

import org.http4k.storyboard.EventContext
import org.http4k.storyboard.FrameExtractor
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level.Detail
import org.http4k.storyboard.util.StoryboardTemplates
import org.http4k.storyboard.util.StoryboardViewModel
import org.http4k.storyboard.util.gzipBase64Encode

object HttpExchangeExtractor : FrameExtractor {
    override operator fun invoke(input: EventContext): StoryFrame? {
        val attrs = input.event.attributes
        val method = attrs["http.request.method"] ?: return null
        val url = attrs["url.full"] ?: attrs["url.path"] ?: ""

        val view = View(
            kind = input.span.kind,
            method = method,
            url = url,
            path = attrs["http.route"] ?: pathOf(url),
            status = attrs["http.response.status_code"] ?: "?",
            statusClass = statusClass(attrs["http.response.status_code"] ?: "?"),
            durationMs = ((input.span.endEpochNanos - input.span.startEpochNanos) / 1_000_000).toString(),
            requestSize = attrs["http.request.body.size"] ?: "0",
            responseSize = attrs["http.response.body.size"] ?: "0"
        )

        return StoryFrame(
            title = "${input.span.kind} $method ${attrs["http.route"] ?: pathOf(url)}",
            notes = "",
            dom = StoryboardTemplates()(view).gzipBase64Encode(),
            level = Detail
        )
    }

    private fun pathOf(url: String): String = when {
        url.isEmpty() -> "/"
        url.startsWith("/") -> url
        else -> ("/" + url.substringAfter("://").substringAfter('/', "")).ifEmpty { "/" }
    }

    private fun statusClass(status: String): String = when {
        status.startsWith("5") -> "s5xx"
        status.startsWith("4") -> "s4xx"
        else -> ""
    }

    data class View(
        val kind: String,
        val method: String,
        val url: String,
        val path: String,
        val status: String,
        val statusClass: String,
        val durationMs: String,
        val requestSize: String,
        val responseSize: String
    ) : StoryboardViewModel
}
