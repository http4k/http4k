package org.http4k.mcp.protocol.messages

import org.http4k.mcp.model.Cursor

interface PaginatedResponse {
    val nextCursor: Cursor?
}
