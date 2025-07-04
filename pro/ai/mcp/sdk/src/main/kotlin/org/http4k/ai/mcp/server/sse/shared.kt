package org.http4k.ai.mcp.server.sse

import org.http4k.lens.Query
import org.http4k.lens.value
import org.http4k.ai.mcp.protocol.SessionId

val sessionId = Query.value(SessionId).optional("sessionId")
