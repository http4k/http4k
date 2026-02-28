package org.http4k.wiretap.openapi

import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun OpenApi(templates: TemplateRenderer) = object : WiretapFunction {
    override fun http(renderer: DatastarElementRenderer) = "openapi" bind Index(templates)
    override fun mcp() = CapabilityPack(emptyList())
}
