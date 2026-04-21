/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Request

/**
 * Responsible for managing the lifecycle of client sessions, including the assignment of
 * transport to session, and the sending of messages to the client.
 */
interface Sessions<Transport> {
    fun retrieveSession(connectRequest: Request): SessionState

    fun respond(transport: Transport, context: ClientRequestContext, message: McpNodeType): McpNodeType
    fun transportFor(context: ClientRequestContext): Transport
    fun onClose(context: ClientRequestContext, fn: () -> Unit)

    fun request(context: ClientRequestContext, message: McpNodeType)

    fun assign(context: ClientRequestContext, transport: Transport, connectRequest: Request)
    fun end(context: ClientRequestContext)
}

