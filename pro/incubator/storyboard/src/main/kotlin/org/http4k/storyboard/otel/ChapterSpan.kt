/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.otel

import io.opentelemetry.api.OpenTelemetry

internal const val STORYBOARD_TRACER = "org.http4k.storyboard"
internal const val CHAPTER_ATTRIBUTE = "storyboard.chapter"

/**
 * Opens a chapter span (tagged `storyboard.chapter = "true"`) on this OTel instance and
 * pushes it onto the current context. Returns a closer lambda that ends the scope and
 * the span; call it when the chapter is done.
 *
 * Used by both the recording session's root span and by `Storyboard.chapter(name) { ... }`
 * — same machinery, different lifecycles.
 */
internal fun OpenTelemetry.openChapterSpan(name: String): () -> Unit {
    val span = getTracer(STORYBOARD_TRACER).spanBuilder(name)
        .setAttribute(CHAPTER_ATTRIBUTE, "true")
        .startSpan()
    val scope = span.makeCurrent()
    return { scope.close(); span.end() }
}
