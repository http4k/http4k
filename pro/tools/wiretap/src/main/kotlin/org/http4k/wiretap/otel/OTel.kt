package org.http4k.wiretap.otel

import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.TraceStore

fun OTel(
    templates: TemplateRenderer,
    traceStore: TraceStore
) = object : WiretapFunction {
    private val functions = listOf(
        ListTraces(traceStore),
        GetTrace(traceStore),
    )

    override fun http(renderer: DatastarElementRenderer) =
        "otel" bind routes(functions.map { it.http(renderer) } + Index(templates))

    override fun mcp() = CapabilityPack(functions.map { it.mcp() })
}
