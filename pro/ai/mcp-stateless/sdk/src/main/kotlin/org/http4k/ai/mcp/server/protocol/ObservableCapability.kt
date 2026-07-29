/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.McpException
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound

/**
 * Provides notification telling the client of changes to underlying capabilities lists.
 */
interface ObservableCapability<T> {
    var items: Iterable<T>

    fun onChange(session: Session, handler: () -> Any): Unit = throw McpException(MethodNotFound)

    fun remove(session: Session): Unit = throw McpException(MethodNotFound)
}
