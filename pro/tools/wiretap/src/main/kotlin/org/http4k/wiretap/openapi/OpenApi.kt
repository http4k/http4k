package org.http4k.wiretap.openapi

import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun OpenApi() = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) = "openapi" bind Index(html)
    override fun mcp() = CapabilityPack(emptyList())
}
