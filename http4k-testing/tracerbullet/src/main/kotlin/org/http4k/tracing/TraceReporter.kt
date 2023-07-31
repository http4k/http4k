package org.http4k.tracing

import org.http4k.core.Uri

/**
 * Report the rendering of a trace
 */
fun interface TraceReporter {
    operator fun invoke(location: Uri, traceCompletion: TraceCompletion, render: TraceRender)

    companion object {
        val NoOp = TraceReporter { _, _, _ -> }
    }
}
