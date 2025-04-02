package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Resource
import org.http4k.mcp.server.protocol.Client
import org.http4k.mcp.server.protocol.Client.Companion.NoOp

/**
 * A resource handler is responsible for loading the content of a Resource
 */
typealias ResourceHandler = (ResourceRequest) -> ResourceResponse

data class ResourceRequest(
    val uri: Uri,
    val meta: Meta = Meta.default,
    val client: Client = NoOp,
    val connectRequest: Request? = null
)

data class ResourceResponse(val list: List<Resource.Content>, val meta: Meta = Meta.default) {
    constructor(vararg content: Resource.Content, meta: Meta = Meta.default) : this(content.toList(), meta)
}
