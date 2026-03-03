package org.http4k.wiretap.mcp.resources

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.core.Method.GET
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.mcp.McpTabResetSignals
import org.http4k.wiretap.mcp.mcpTabResponse

data class McpResourceView(
    val uri: String,
    val name: String,
    val description: String,
    val mimeType: String
)

data class McpResourceTemplateView(
    val uriTemplate: String,
    val name: String,
    val description: String,
    val mimeType: String
)

fun McpResource.toResourceView() = McpResourceView(
    uri = uri?.toString() ?: "",
    name = name.value,
    description = description ?: "",
    mimeType = mimeType?.value ?: ""
)

fun McpResource.toTemplateView() = McpResourceTemplateView(
    uriTemplate = uriTemplate?.value ?: "",
    name = name.value,
    description = description ?: "",
    mimeType = mimeType?.value ?: ""
)

data class TabContent(
    val resources: List<McpResourceView>,
    val templates: List<McpResourceTemplateView>
) : ViewModel

fun TabContent(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/resources" bind GET to {
        val resources = mcpClient.resources().list()
            .map { list -> list.map { r -> r.toResourceView() } }
            .valueOrNull() ?: emptyList()

        val templates = mcpClient.resources().listTemplates()
            .map { list -> list.map { r -> r.toTemplateView() } }
            .valueOrNull() ?: emptyList()

        elements.mcpTabResponse(TabContent(resources, templates), McpTabResetSignals())
    }
