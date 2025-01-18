package org.http4k.connect.mcp

interface PaginatedRequest: ClientMessage.Request {
    val cursor: Cursor?
}

