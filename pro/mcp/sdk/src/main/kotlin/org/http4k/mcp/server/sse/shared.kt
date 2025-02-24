package org.http4k.mcp.server.sse

import org.http4k.lens.Query
import org.http4k.lens.value
import org.http4k.mcp.protocol.SessionId

val sessionId = Query.value(SessionId).required("sessionId")
