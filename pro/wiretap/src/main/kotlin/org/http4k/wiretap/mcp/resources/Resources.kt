/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp.resources

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun Resources(mcpClient: McpClient) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) = "/resources" bind routes(
        InspectResource(mcpClient, elements),
        InspectTemplate(mcpClient, elements),
        ReadResource(mcpClient, elements),
        TabContent(mcpClient, elements),
    )

    override fun mcp() = CapabilityPack()
}
