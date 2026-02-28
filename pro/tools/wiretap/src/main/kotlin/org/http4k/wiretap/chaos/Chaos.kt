package org.http4k.wiretap.chaos

import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.chaos.ChaosEngine
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun Chaos(inboundChaos: ChaosEngine, outboundChaos: ChaosEngine) =
    object : WiretapFunction {
        private val functions = listOf(
            ChaosStatus(inboundChaos, outboundChaos),
            ChaosActivate(inboundChaos, outboundChaos),
            ChaosDeactivate(inboundChaos, outboundChaos)
        )

        override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
            "chaos" bind routes(functions.map { it.http(elements, html) } + Index(html))

        override fun mcp() = CapabilityPack(functions.map { it.mcp() })
    }
