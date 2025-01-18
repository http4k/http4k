package org.http4k.routing

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.mcp.MimeType
import org.http4k.mcp.model.Resource

class RoutedResource(val uri: Uri) : McpRouting {
    fun toResource() = Resource(
        uri,
        uri.host,
        "description",
        MimeType.of(APPLICATION_JSON)
    )

    fun read() = listOf(
        Resource.Content.Text("asd", uri, MimeType.of(APPLICATION_JSON))
    )
}
