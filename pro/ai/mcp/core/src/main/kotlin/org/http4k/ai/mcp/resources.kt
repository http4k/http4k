package org.http4k.ai.mcp

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.Client.Companion.NoOp

/**
 * A resource handler is responsible for loading the content of a Resource
 */
typealias ResourceHandler = (ResourceRequest) -> ResourceResponse

fun interface ResourceFilter {
    operator fun invoke(request: ResourceHandler): ResourceHandler
    companion object
}

val ResourceFilter.Companion.NoOp: ResourceFilter get() = ResourceFilter { it }

fun ResourceFilter.then(next: ResourceFilter): ResourceFilter = ResourceFilter { this(next(it)) }

fun ResourceFilter.then(next: ResourceHandler): ResourceHandler = this(next)

data class ResourceRequest(
    val uri: Uri,
    val meta: Meta = Meta.default,
    val client: Client = NoOp,
    val connectRequest: Request? = null
)

data class ResourceResponse(val list: List<Resource.Content>, val meta: Meta = Meta.default) {
    constructor(vararg content: Resource.Content, meta: Meta = Meta.default) : this(content.toList(), meta)
}
