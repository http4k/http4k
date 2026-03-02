package org.http4k.wiretap.mcp

import org.http4k.ai.mcp.client.http.HttpNonStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.mcp.apps.Apps
import org.http4k.wiretap.mcp.client.InboundClient

fun Mcp(uri: Uri, rawClient: HttpHandler, proxy: HttpHandler) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/mcp" bind when {
            mcpAvailable(rawClient, uri) -> {
                val mcpClient = HttpNonStreamingMcpClient(
                    McpEntity.of("http4k Wiretap"), Version.of("0.0.0"),
                    uri.path("/mcp"), proxy
                )

                routes(
                    "/app" bind Apps(mcpClient).http(elements, html),
                    "/inbound" bind InboundClient(mcpClient).http(elements, html)
                )
            }

            else -> routes(emptyList())
        }

    override fun mcp() = CapabilityPack()
}

private fun mcpAvailable(rawClient: HttpHandler, uri: Uri): Boolean =
    rawClient(Request(GET, uri.path("/mcp"))).status.successful
