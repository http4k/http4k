package org.http4k.tracing

fun interface TraceRenderPersistence {
    operator fun invoke(render: TraceRender)

    companion object
}

