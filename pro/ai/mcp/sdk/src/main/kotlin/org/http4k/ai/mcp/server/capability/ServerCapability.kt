/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

/**
 * Marker interface for classes which are used to bind capabilities to the MCP server.
 */
sealed interface ServerCapability : Iterable<ServerCapability> {
    val name: String
    override fun iterator() = listOf(this).iterator()
}

