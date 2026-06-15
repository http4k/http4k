package org.http4k.storyboard.otel

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

class StoryboardSpanExporter(private val store: SpanSnapshotStore) : SpanExporter {
    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        spans.forEach { store.record(it) }
        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
}
