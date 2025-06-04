package org.http4k.mcp.server.protocol

import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.mcp.protocol.McpException

/**
 * Provides notification telling the client of changes to underlying capabilities lists.
 */
interface ObservableCapability {
    fun onChange(session: Session, handler: () -> Any): Unit = throw McpException(MethodNotFound)

    fun remove(session: Session): Unit = throw McpException(MethodNotFound)
}
