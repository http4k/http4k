package org.http4k.wiretap

import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.routing.RoutingHttpHandler
import org.http4k.template.DatastarElementRenderer

interface WiretapFunction {
    fun http(renderer: DatastarElementRenderer): RoutingHttpHandler
    fun mcp(): ServerCapability
}
