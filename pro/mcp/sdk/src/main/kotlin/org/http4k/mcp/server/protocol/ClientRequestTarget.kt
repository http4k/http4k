package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.ProgressToken

/**
 * How a client is identified for sending a request to.
 */
sealed interface ClientRequestTarget {
    data class Request(val progressToken: ProgressToken) : ClientRequestTarget
    data class Entity(val entity: McpEntity) : ClientRequestTarget
}
