package org.http4k.tracing

import org.http4k.core.Uri

/**
 * Persists the trace and optionally reports the location
 */
fun interface TraceRenderPersistence : (TraceRender) -> Uri? {
    companion object
}
