package org.http4k.wiretap.mcp.resources

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel

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

fun ResourcesTabContent(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/tab/resources" bind GET to {
        val resources = mcpClient.resources().list()
            .map { list -> list.map { r -> r.toResourceView() } }
            .valueOrNull() ?: emptyList()

        val templates = mcpClient.resources().listTemplates()
            .map { list -> list.map { r -> r.toTemplateView() } }
            .valueOrNull() ?: emptyList()

        Response(OK).datastarElements(
            elements(TabContent(resources, templates)),
            selector = Selector.of("#mcp-content")
        )
    }
