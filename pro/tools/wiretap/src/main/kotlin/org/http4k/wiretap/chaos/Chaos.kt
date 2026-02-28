package org.http4k.wiretap.chaos

import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.chaos.ChaosEngine
import org.http4k.filter.debug
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun Chaos(templates: TemplateRenderer, inboundChaos: ChaosEngine, outboundChaos: ChaosEngine) =
    object : WiretapFunction {
        private val functions = listOf(
            ChaosStatus(inboundChaos, outboundChaos),
            ChaosActivate(inboundChaos, outboundChaos),
            ChaosDeactivate(inboundChaos, outboundChaos)
        )

        override fun http(renderer: DatastarElementRenderer) =
            "chaos" bind routes(functions.map { it.http(renderer) } + Index(templates))

        override fun mcp() = CapabilityPack(functions.map { it.mcp() })
    }
