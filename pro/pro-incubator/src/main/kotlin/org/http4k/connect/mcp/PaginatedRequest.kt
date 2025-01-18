package org.http4k.connect.mcp

import org.http4k.mcp.model.Cursor

interface PaginatedRequest: ClientMessage.Request {
    val cursor: Cursor?
}

