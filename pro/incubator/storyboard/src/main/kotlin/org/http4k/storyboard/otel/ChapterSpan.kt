/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.otel

import io.opentelemetry.api.OpenTelemetry

const val STORYBOARD_TRACER = "org.http4k.storyboard"
const val CHAPTER_ATTRIBUTE = "storyboard.chapter"

fun OpenTelemetry.openChapterSpan(name: String): () -> Unit {
    val span = getTracer(STORYBOARD_TRACER).spanBuilder(name)
        .setAttribute(CHAPTER_ATTRIBUTE, "true")
        .startSpan()
    val scope = span.makeCurrent()
    return { scope.close(); span.end() }
}
