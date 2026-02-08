package org.http4k.ai.mcp.apps.server

import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Resource.Content.Text
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.extension.CspDomain
import org.http4k.ai.mcp.model.extension.McpAppCsp
import org.http4k.ai.mcp.model.extension.McpAppResourceMeta
import org.http4k.ai.mcp.model.extension.McpApps
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

object OrderFormUi {
    val uri = Uri.of("ui://order-form")

    fun resource(templates: TemplateRenderer) = Resource.Static(
        uri = uri,
        name = ResourceName.of("Order Form"),
        description = "Interactive order form",
        mimeType = McpApps.MIME_TYPE,
    ) bind {
        ResourceResponse(
            Text(
                templates(Form()),
                it.uri,
                McpApps.MIME_TYPE,
                McpAppResourceMeta(McpAppCsp(resourceDomains = listOf(CspDomain.of("https://unpkg.com"))))
            )
        )
    }
}

class Form() : ViewModel
