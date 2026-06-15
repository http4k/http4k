package org.http4k.storyboard.otel

import io.opentelemetry.sdk.trace.data.SpanData
import java.util.concurrent.ConcurrentLinkedQueue

class SpanSnapshotStore {
    private val snapshots = ConcurrentLinkedQueue<SpanSnapshot>()

    fun record(span: SpanData) {
        snapshots += span.toSnapshot()
    }

    fun drain() = snapshots.toList()
}
