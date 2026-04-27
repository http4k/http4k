/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.capability

/**
 * Marker interface for classes which are used to bind capabilities to the A2A server.
 */
sealed interface ServerCapability : Iterable<ServerCapability> {
    val name: String
    override fun iterator() = listOf(this).iterator()
}
