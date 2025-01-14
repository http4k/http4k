package org.http4k.mcp

import org.http4k.connect.mcp.Resource
import org.http4k.core.ContentType
import org.http4k.core.Uri

class ResourceTemplateBinding(val uri: Uri) : McpBinding {
    fun toTemplate() = Resource.Template(
        uri,
        uri.host,
        "description",
        MimeType.of(ContentType.APPLICATION_JSON)
    )
}
