/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import org.http4k.storyboard.extractor.FallbackFrameExtractor
import org.http4k.storyboard.extractor.HttpExchangeExtractor
import org.http4k.storyboard.extractor.SqlExtractor
import org.http4k.storyboard.otel.SpanSnapshotStore
import org.http4k.storyboard.otel.StoryboardOpenTelemetry
import org.http4k.storyboard.otel.openChapterSpan
import org.http4k.storyboard.render.buildChapters
import java.time.Clock
import java.time.Instant

class Storyboard internal constructor(
    val name: String,
    val series: String? = null,
    private val clock: Clock = Clock.systemUTC()
) {
    private val spanStore = SpanSnapshotStore()
    internal val otel: OpenTelemetry = StoryboardOpenTelemetry(spanStore, clock)
    private val closeRootSession: () -> Unit = otel.openChapterSpan(name)
    private val started: Instant = clock.instant()

    /** Runs [block] against this recorder, ending the session in a `finally` so the root span always closes. */
    operator fun invoke(block: Storyboard.() -> Unit): Storyboard {
        try {
            block()
        } finally {
            endSession()
        }
        return this
    }

    /**
     * Capture a frame of the [Chapter].
     */
    fun capture(frame: StoryFrame) {
        Span.current().addEvent("storyboard.frame", frame.toEventAttributes().toOtelAttributes())
    }

    /**
     * Open a named section and process the [block] within it.
     */
    fun chapter(name: String, block: () -> Unit) {
        val close = otel.openChapterSpan(name)
        try {
            block()
        } finally {
            close()
        }
    }

    internal fun endSession() {
        closeRootSession()
    }

    internal fun toStory(outcome: Story.Outcome? = null, extractors: List<FrameExtractor>) = Story(
        title = name,
        series = series,
        chapters = buildChapters(spanStore.snapshots, extractors + FallbackFrameExtractor),
        outcome = outcome,
        durationMs = clock.instant().toEpochMilli() - started.toEpochMilli()
    )
}

/**
 * Programmatic entry: opens a [Storyboard], runs [block] against it and returns the rendered [Story].
 */
fun storyboard(
    name: String,
    series: String? = null,
    extractors: List<FrameExtractor> = defaultExtractors,
    clock: Clock = Clock.systemUTC(),
    block: Storyboard.() -> Unit
) = Storyboard(name, series, clock)(block).toStory(extractors = extractors)

private fun Map<String, String>.toOtelAttributes(): Attributes =
    entries.fold(Attributes.builder()) { acc, (k, v) -> acc.put(k, v) }.build()

val defaultExtractors = listOf(HttpExchangeExtractor, SqlExtractor)
