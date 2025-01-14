package org.http4k.mcp

import org.http4k.connect.mcp.Resource
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri

class ResourceBinding(val uri: Uri) : McpBinding {
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
