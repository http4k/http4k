package org.http4k.ai.mcp.server.protocol

import org.http4k.core.Uri
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpResource

/**
 * Provides notification telling the client of changes to a particular resource
 */
interface ObservableResources : Resources {
    fun triggerUpdated(uri: Uri): Unit = throw McpException(MethodNotFound)

    fun subscribe(session: Session, req: McpResource.Subscribe.Request, fn: (Uri) -> Unit): Unit =
        throw McpException(MethodNotFound)

    fun unsubscribe(session: Session, req: McpResource.Unsubscribe.Request): Unit = throw McpException(MethodNotFound)
}
