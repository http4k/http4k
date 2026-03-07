package org.http4k.wiretap.mcp.resources

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.Path
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel

data class TemplateDetailView(
    val uriTemplate: String,
    val name: String,
    val description: String,
    val mimeType: String
) : ViewModel

fun InspectTemplate(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/templates/{name}" bind GET to { req ->
        val name = Path.of("name")(req)
        val template = mcpClient.resources().listTemplates()
            .map { list -> list.first { it.name.value == name } }
            .valueOrNull()

        when (template) {
            null -> Response(NOT_FOUND)
            else -> Response(OK).datastarElements(
                elements(
                    TemplateDetailView(
                        template.uriTemplate?.value ?: "",
                        template.name.value,
                        template.description ?: "",
                        template.mimeType?.value ?: ""
                    )
                ),
                selector = Selector.of("#detail-panel")
            )
        }
    }
