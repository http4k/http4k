package org.http4k.wiretap.mcp

import org.http4k.ai.mcp.client.http.HttpNonStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun Mcp(uri: Uri, rawClient: HttpHandler, proxy: HttpHandler) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer): RoutingHttpHandler {
        val mcpServerUri = uri.extend(Uri.of("/mcp"))

        val mcpClient = HttpNonStreamingMcpClient(
            McpEntity.of("http4k Wiretap"), Version.of("0.0.0"),
            mcpServerUri, proxy
        )

        val functions = when {
            rawClient.mcpAvailable(mcpServerUri) -> listOf(InboundClient(mcpClient))
            else -> emptyList()
        }

        return "/mcp" bind routes(functions.map { it.http(elements, html) })
    }

    override fun mcp() = CapabilityPack()
}

private fun HttpHandler.mcpAvailable(baseUri: Uri): Boolean =
    this(Request(GET, baseUri)).status.successful
