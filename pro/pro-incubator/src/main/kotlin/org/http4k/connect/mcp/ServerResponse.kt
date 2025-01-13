package org.http4k.connect.mcp

interface ServerResponse {
    object Empty : ServerResponse
}

data object NoResponse : ServerResponse
