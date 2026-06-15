package org.http4k.storyboard.otel

import io.opentelemetry.sdk.trace.data.SpanData
import java.util.concurrent.ConcurrentLinkedQueue

class SpanSnapshotStore {
    private val recorded = ConcurrentLinkedQueue<SpanSnapshot>()

    fun record(span: SpanData) {
        recorded += span.toSnapshot()
    }

    fun drain() = recorded.toList()
}
