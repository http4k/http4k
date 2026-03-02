package org.http4k.wiretap.mcp.client.resources

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

data class ResourceDetailView(
    val uri: String,
    val name: String,
    val description: String,
    val mimeType: String
) : ViewModel

fun InspectResource(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/detail/resources/{name}" bind GET to { req ->
        val name = Path.of("name")(req)
        val resource = mcpClient.resources().list()
            .map { list -> list.first { it.name.value == name } }
            .valueOrNull()

        when (resource) {
            null -> Response(NOT_FOUND)
            else -> Response(OK).datastarElements(
                elements(
                    ResourceDetailView(
                        resource.uri?.toString() ?: "",
                        resource.name.value,
                        resource.description ?: "",
                        resource.mimeType?.value ?: ""
                    )
                ),
                selector = Selector.of("#detail-panel")
            )
        }
    }
