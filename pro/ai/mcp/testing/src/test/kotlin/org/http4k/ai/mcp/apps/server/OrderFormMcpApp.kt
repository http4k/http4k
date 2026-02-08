package org.http4k.ai.mcp.apps.server

import org.http4k.ai.mcp.model.Domain
import org.http4k.ai.mcp.model.apps.Csp
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.ai.mcp.server.capability.extension.RenderMcpApp
import org.http4k.core.Uri
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel

fun OrderFormMcpApp(): CapabilityPack {
    val templates = HandlebarsTemplates().CachingClasspath()
    val renderApp = RenderMcpApp(
        name = "show_order_form",
        description = "Display the order form UI",
        uri = Uri.of("ui://order-form"),
        meta = McpAppResourceMeta(csp = Csp(resourceDomains = listOf(Domain.of("https://unpkg.com"))))
    ) { templates(Form()) }

    return CapabilityPack(renderApp, SubmitOrderTool())
}

class Form() : ViewModel
