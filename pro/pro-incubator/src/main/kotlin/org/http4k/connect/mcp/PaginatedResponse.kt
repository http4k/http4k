package org.http4k.connect.mcp

interface PaginatedResponse {
    val nextCursor: Cursor?
}
