/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.render

import org.http4k.storyboard.Chapter
import org.http4k.storyboard.EventContext
import org.http4k.storyboard.FrameExtractor
import org.http4k.storyboard.otel.CHAPTER_ATTRIBUTE
import org.http4k.storyboard.otel.SpanSnapshot

/**
 * Pure transform: raw OTel-style spans → nested [Chapter] tree, with frame events run
 * through the [extractors] chain. Non-chapter spans surface as a single virtual event so
 * extractors that key off span attributes (e.g. HTTP exchanges) still fire.
 */
internal fun buildChapters(spans: List<SpanSnapshot>, extractors: List<FrameExtractor>): List<Chapter> {
    val byId = spans.associateBy { it.spanId }
    fun isChapter(span: SpanSnapshot) = span.attributes[CHAPTER_ATTRIBUTE] == "true"

    fun nearestChapterAncestor(start: SpanSnapshot): SpanSnapshot? {
        var current = start.parentSpanId?.let { byId[it] }
        while (current != null) {
            if (isChapter(current)) return current
            current = current.parentSpanId?.let { byId[it] }
        }
        return null
    }

    val chapterSpans = spans.filter(::isChapter)
    if (chapterSpans.isEmpty()) return emptyList()

    val childrenByParent = chapterSpans
        .mapNotNull { span -> nearestChapterAncestor(span)?.let { it.spanId to span } }
        .groupBy({ it.first }, { it.second })

    val eventsByChapter = spans
        .flatMap { span ->
            val real = span.events.map { span to it }
            if (isChapter(span)) real
            else real + (span to span.asVirtualEvent())
        }
        .mapNotNull { (span, event) ->
            val chapter = if (isChapter(span)) span else nearestChapterAncestor(span)
            chapter?.let { it.spanId to (span to event) }
        }
        .groupBy({ it.first }, { it.second })

    fun toChapter(span: SpanSnapshot): Chapter = Chapter(
        title = span.name,
        frames = eventsByChapter[span.spanId].orEmpty()
            .mapNotNull { (originSpan, event) ->
                val context = EventContext(originSpan, event)
                extractors.firstNotNullOfOrNull { it(context) }
            },
        children = childrenByParent[span.spanId].orEmpty()
            .sortedBy { it.startEpochNanos }
            .map(::toChapter)
    )

    return chapterSpans
        .filter { nearestChapterAncestor(it) == null }
        .sortedBy { it.startEpochNanos }
        .map(::toChapter)
}

private fun SpanSnapshot.asVirtualEvent(): SpanSnapshot.Event = SpanSnapshot.Event(
    name = name,
    epochNanos = startEpochNanos,
    attributes = attributes
)
