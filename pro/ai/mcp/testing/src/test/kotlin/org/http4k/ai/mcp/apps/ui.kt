package org.http4k.ai.mcp.apps

import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Resource.Content.Text
import org.http4k.ai.mcp.model.Resource.Static
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.extension.McpApps
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

object OrderFormUi {
    val uri = Uri.of("ui://order-form")

    fun resource(templates: TemplateRenderer) = Static(
        uri = uri,
        name = ResourceName.of("Order Form"),
        description = "Interactive order form",
        mimeType = McpApps.MIME_TYPE
    ) bind { ResourceResponse(Text(templates(Form()), it.uri, McpApps.MIME_TYPE)) }
}

class Form() : ViewModel
