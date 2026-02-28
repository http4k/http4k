package org.http4k.wiretap.otel

import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.TraceStore

fun OTel(traceStore: TraceStore) = object : WiretapFunction {
    private val functions = listOf(
        ListTraces(traceStore),
        GetTrace(traceStore),
    )

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "otel" bind routes(functions.map { it.http(elements, html) } + Index(html))

    override fun mcp() = CapabilityPack(functions.map { it.mcp() })
}
