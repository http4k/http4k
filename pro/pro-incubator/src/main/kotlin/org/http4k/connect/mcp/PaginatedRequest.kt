package org.http4k.connect.mcp

typealias Cursor = String

interface PaginatedRequest {
    val cursor: Cursor?
}

