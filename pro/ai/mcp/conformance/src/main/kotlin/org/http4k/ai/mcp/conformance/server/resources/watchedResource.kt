package org.http4k.ai.mcp.conformance.server.resources

import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.routing.bind

fun watchedResource() =
    Resource.Static(Uri.of("test://watched-resource"), ResourceName.of("watched-resource"), null, MimeType.TEXT_PLAIN) bind {
        ResourceResponse(
            listOf(
                Resource.Content.Text(
                    "Watched resource content.",
                    Uri.of("test://watched-resource"),
                    MimeType.TEXT_PLAIN
                )
            )
        )
    }
