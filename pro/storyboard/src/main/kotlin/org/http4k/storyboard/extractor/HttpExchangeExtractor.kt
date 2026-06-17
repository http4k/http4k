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
        val route = attrs["http.route"]
        val url = attrs["url.full"] ?: attrs["url.path"] ?: ""
        val status = attrs["http.response.status_code"] ?: "?"
        val kind = input.span.kind
        val durationMs = ((input.span.endEpochNanos - input.span.startEpochNanos) / 1_000_000).toString()
        val reqSize = attrs["http.request.body.size"] ?: "0"
        val respSize = attrs["http.response.body.size"] ?: "0"
        val path = route ?: pathOf(url)

        val view = View(
            kind = kind,
            method = method,
            url = url,
            path = path,
            status = status,
            statusClass = statusClass(status),
            durationMs = durationMs,
            requestSize = reqSize,
            responseSize = respSize
        )

        return StoryFrame(
            title = "$kind $method $path",
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
