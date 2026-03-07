package org.http4k.wiretap.otel

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.http4k.wiretap.domain.TraceStore

class WiretapSpanExporter(private val traceStore: TraceStore) : SpanExporter {
    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        spans.forEach { traceStore.record(it) }
        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
}
