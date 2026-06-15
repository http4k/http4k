package org.http4k.storyboard.otel

import io.opentelemetry.sdk.trace.data.SpanData
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Spans collected during a single recording session.
 */
class SpanSnapshotStore {
    private val spans = ConcurrentLinkedQueue<SpanSnapshot>()

    fun record(span: SpanData) {
        spans += span.toSnapshot()
    }

    fun drain() = spans.toList()
}
