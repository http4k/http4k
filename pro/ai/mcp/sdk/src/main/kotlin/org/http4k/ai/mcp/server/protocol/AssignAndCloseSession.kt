/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.server.protocol.ClientRequestContext.ClientCall

fun <Transport> AssignAndCloseSession(sessions: Sessions<Transport>, transport: Transport) = McpFilter { next ->
    {
        val context = ClientCall(it.session)
        try {
            sessions.assign(context, transport, it.http)
            next(it).also {
                sessions.respond(transport, context, it.json)
            }
        } finally {
            sessions.end(context)
        }
    }
}
