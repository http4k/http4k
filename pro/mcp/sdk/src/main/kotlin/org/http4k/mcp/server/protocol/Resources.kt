package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.protocol.messages.McpResource

/**
 * Handles protocol traffic for resources features and subscriptions.
 */
interface Resources {
    fun triggerUpdated(uri: Uri)

    fun listResources(req: McpResource.List.Request, client: Client, http: Request): McpResource.List.Response

    fun listTemplates(req: McpResource.Template.List.Request, client: Client, http: Request): McpResource.Template.List.Response

    fun read(req: McpResource.Read.Request, client: Client, http: Request): McpResource.Read.Response

    fun subscribe(session: Session, req: McpResource.Subscribe.Request, fn: (Uri) -> Unit)

    fun unsubscribe(session: Session, req: McpResource.Unsubscribe.Request)

    fun onChange(session: Session, handler: () -> Any)

    fun remove(session: Session)
}
