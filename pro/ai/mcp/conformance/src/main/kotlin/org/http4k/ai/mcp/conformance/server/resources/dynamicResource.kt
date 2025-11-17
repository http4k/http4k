package org.http4k.ai.mcp.conformance.server.resources

import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.routing.bind

fun dynamicResource() =
    Resource.Static(Uri.of("test://dynamic-resource"), ResourceName.of("dynamic-resource"), null, MimeType.TEXT_PLAIN) bind {
        throw NotImplementedError()
    }
