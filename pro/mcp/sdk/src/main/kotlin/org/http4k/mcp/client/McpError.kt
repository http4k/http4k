package org.http4k.mcp.client

import org.http4k.core.Response
import org.http4k.jsonrpc.ErrorMessage

/**
 * Sealed type encapsulating the known failure modes of MCP clients
 */
sealed interface McpError {

    /**
     * Standard error returned within the MCP protocol
     */
    data class Protocol(val error: ErrorMessage) : McpError

    /**
     * HTTP error, most commonly thrown during sending of a request
     */
    data class Http(val response: Response) : McpError

    /**
     * It's a timeout when waiting for a response to complete
     */
    data object Timeout : McpError

    /**
     * Unexpected error
     */
    data class Internal(val cause: Exception) : McpError
}
