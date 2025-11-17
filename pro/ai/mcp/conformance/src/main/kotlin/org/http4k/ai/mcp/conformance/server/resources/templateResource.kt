package org.http4k.ai.mcp.conformance.server.resources

import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.ResourceUriTemplate
import org.http4k.connect.model.MimeType.Companion.APPLICATION_JSON
import org.http4k.routing.bind

fun templateResource() = Resource.Templated(
    ResourceUriTemplate.of("test://template/{id}/data"),
    ResourceName.of("template"),
    null,
    APPLICATION_JSON
) bind {
    ResourceResponse(
        listOf(
            Resource.Content.Text(
                "content for ${it.uri}",
                it.uri,
                APPLICATION_JSON
            )
        )
    )
}
