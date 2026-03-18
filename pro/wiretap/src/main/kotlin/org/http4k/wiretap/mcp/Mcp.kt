/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp

import org.http4k.ai.mcp.client.http.HttpNonStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.Wiretapped

fun Mcp(
    wiretapped: Wiretapped,
    path: String,
    rawClient: HttpHandler,
    proxy: HttpHandler
) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer): RoutingHttpHandler {
        val mcpClient = HttpNonStreamingMcpClient(
            Uri.of(path),
            McpEntity.of("http4k-wiretap"), Version.of("0.0.0"),
            proxy
        )

        return "/mcp" bind when {
            wiretapped.supportsMcp(rawClient, path) -> InboundClient(mcpClient).http(elements, html)
            else -> Index(html, NoMcpServerFound)
        }
    }

    override fun mcp() = CapabilityPack()
}


private fun mcpAvailable(http: HttpHandler, baseUri: Uri): Boolean =
    http(Request(GET, baseUri)).status.successful
