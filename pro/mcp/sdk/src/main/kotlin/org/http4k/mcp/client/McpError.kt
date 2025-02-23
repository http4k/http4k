package org.http4k.mcp.client

import org.http4k.core.Response
import org.http4k.jsonrpc.ErrorMessage

/**
 * Sealed type encapsulating the known failure modes of MCP clients
 */
sealed class McpError {
    data class Server(val error: ErrorMessage) : McpError()
    data class Internal(val e: Exception) : McpError()
    data class Http(val response: Response) : McpError()
    data object Timeout : McpError()
}
