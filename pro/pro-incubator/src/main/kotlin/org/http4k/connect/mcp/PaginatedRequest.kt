package org.http4k.connect.mcp

typealias Cursor = String

interface PaginatedRequest: ClientMessage.Request {
    val cursor: Cursor?
}

