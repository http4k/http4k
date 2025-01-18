package org.http4k.mcp.protocol

import org.http4k.mcp.model.Cursor

interface PaginatedResponse {
    val nextCursor: Cursor?
}
