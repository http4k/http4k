package org.http4k.wiretap.mcp.client.resources

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json

data class McpClientResourcesSignals(val selectedItem: String = "")

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

fun Index(mcpClient: McpClient, html: TemplateRenderer) = "/resources" bind GET to {
    val resources = mcpClient.resources().list()
        .map { list -> list.map { r -> r.toResourceView() } }
        .valueOrNull() ?: emptyList()

    val templates = mcpClient.resources().listTemplates()
        .map { list -> list.map { r -> r.toTemplateView() } }
        .valueOrNull() ?: emptyList()

    Response(OK).html(html(Index(resources, templates)))
}

data class Index(
    val resources: List<McpResourceView>,
    val templates: List<McpResourceTemplateView>
) : ViewModel {
    val initialSignals: String = Json.asFormatString(McpClientResourcesSignals())
}

private fun McpResource.toResourceView() = McpResourceView(
    uri = uri?.toString() ?: "",
    name = name.value,
    description = description ?: "",
    mimeType = mimeType?.value ?: ""
)

private fun McpResource.toTemplateView() = McpResourceTemplateView(
    uriTemplate = uriTemplate?.value ?: "",
    name = name.value,
    description = description ?: "",
    mimeType = mimeType?.value ?: ""
)
