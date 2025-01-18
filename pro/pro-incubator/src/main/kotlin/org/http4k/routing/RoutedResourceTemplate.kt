package org.http4k.routing

import org.http4k.connect.mcp.protocol.McpResource
import org.http4k.core.ContentType
import org.http4k.core.Uri
import org.http4k.mcp.MimeType

class RoutedResourceTemplate(val uri: Uri) : McpRouting {
    fun toTemplate() = McpResource.Template(
        uri,
        uri.host,
        "description",
        MimeType.of(ContentType.APPLICATION_JSON)
    )
}
