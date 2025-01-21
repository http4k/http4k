package org.http4k.mcp.protocol

import org.http4k.mcp.model.Cursor

interface PaginatedRequest: ClientMessage.Request {
    val cursor: Cursor?
}

