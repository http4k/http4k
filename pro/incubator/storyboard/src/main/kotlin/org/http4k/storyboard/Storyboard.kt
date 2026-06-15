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

/**
 * The recorder. Owns an isolated OpenTelemetry SDK for the duration of a single
 * recording session: opens a root chapter span on construction, captures frame
 * events on whatever span is current, and at the end runs the collected spans
 * through the extractor chain to build a [Story].
 *
 * Not constructed directly — use the top-level [storyboard] block function (for
 * programmatic recordings) or `RenderStoryboard` (the JUnit5 extension). Both
 * route through the same internal lifecycle.
 *
 * The public API:
 *  - [captureFrame] — emit a [StoryFrame] onto the current chapter span
 *  - [chapter] — open a named sub-section; events captured inside attach to it
 *
 * Plugin extensions (in the same package) — [html], [code] — call [captureFrame]
 * for the common variants without you needing to construct frames yourself.
 *
 * @property name shown as the [Story.title] (and used as the root chapter title)
 * @property series optional parent grouping shown as a breadcrumb in the report header
 */
class Storyboard internal constructor(
    val name: String,
    val series: String? = null,
    private val clock: Clock = Clock.systemUTC()
) {
    private val spanStore = SpanSnapshotStore()
    internal val otel: OpenTelemetry = StoryboardOpenTelemetry(spanStore, clock)
    private val closeRootSession: () -> Unit = otel.openChapterSpan(name)
    private val started: Instant = clock.instant()

    /** Runs [fn] against this recorder, ending the session in a `finally` so the root span always closes. */
    operator fun invoke(fn: Storyboard.() -> Unit): Storyboard {
        try {
            fn()
        } finally {
            endSession()
        }
        return this
    }

    /**
     * Emit [frame] as an OTel event on the currently-open chapter span. Frame
     * authors normally call the [html] / [code] extensions or use a
     * [StoryboardWebDriver] instead — this is the low-level escape hatch.
     */
    fun captureFrame(frame: StoryFrame) {
        Span.current().addEvent("storyboard.frame", frame.toEventAttributes().toOtelAttributes())
    }

    /**
     * Open a named sub-section. Frames captured inside [block] attach to a chapter
     * with this [name]; nested calls produce nested chapters. The chapter span is
     * always closed even if [block] throws.
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
        chapters = buildChapters(spanStore.drain(), extractors + FallbackFrameExtractor),
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
