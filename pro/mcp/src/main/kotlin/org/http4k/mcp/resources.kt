package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Resource
import org.http4k.mcp.protocol.HasMeta

/**
 * A resource handler is responsible for loading the content of a Resource
 */
typealias ResourceHandler = (ResourceRequest) -> ResourceResponse

data class ResourceRequest(val uri: Uri, val connectRequest: Request)

data class ResourceResponse(val list: List<Resource.Content>, val meta: Meta = HasMeta.default)
