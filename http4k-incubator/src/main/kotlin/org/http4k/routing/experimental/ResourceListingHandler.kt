package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import java.io.ByteArrayInputStream


class ResourceListingHandler(
    private val dir: ResourceSummary,
    private val contents: Iterable<ResourceSummary>,
    private val renderer: DirectoryRenderer
) : HttpHandler {

    override fun invoke(request: Request): Response =
        ResourceInfoResource(dir, renderer(request.uri, dir, contents).toByteArray(Charsets.UTF_8), ContentType.TEXT_HTML).invoke(request)
}

/**
 * Used so that the If-Modified-Since handling works
 */
private class ResourceInfoResource(resourceSummary: ResourceSummary, val content: ByteArray, override val contentType: ContentType) : Resource {

    override fun openStream() = ByteArrayInputStream(content)

    override val length = content.size.toLong()

    override val lastModified = resourceSummary.lastModified
}
