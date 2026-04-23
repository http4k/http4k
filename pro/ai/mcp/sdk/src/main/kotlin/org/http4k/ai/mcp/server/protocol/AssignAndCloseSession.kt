/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.server.protocol.ClientRequestContext.ClientCall
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.util.McpJson

fun <Transport> AssignAndCloseSession(sessions: Sessions<Transport>, transport: Transport) = McpFilter { next ->
    {
        val context = ClientCall(it.session)
        try {
            sessions.assign(context, transport, it.http)
            next(it).also {
                if (it is Ok) sessions.respond(transport, context, McpJson.asJsonObject(it.message))
            }
        } finally {
            sessions.end(context)
        }
    }
}
