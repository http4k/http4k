package org.http4k.wiretap

import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.routing.RoutingHttpHandler
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer

interface WiretapFunction {
    fun http(elements: DatastarElementRenderer, html: TemplateRenderer): RoutingHttpHandler
    fun mcp(): ServerCapability
}
