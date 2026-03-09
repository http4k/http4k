/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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
            Status(inboundChaos, outboundChaos),
            Activate(inboundChaos, outboundChaos),
            Deactivate(inboundChaos, outboundChaos)
        )

        override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
            "chaos" bind routes(functions.map { it.http(elements, html) } + Index(html))

        override fun mcp() = CapabilityPack(functions.map { it.mcp() })
    }
