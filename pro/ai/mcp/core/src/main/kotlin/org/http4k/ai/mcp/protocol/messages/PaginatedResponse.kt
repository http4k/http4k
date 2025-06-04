package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Cursor

interface PaginatedResponse {
    val nextCursor: Cursor?
}
