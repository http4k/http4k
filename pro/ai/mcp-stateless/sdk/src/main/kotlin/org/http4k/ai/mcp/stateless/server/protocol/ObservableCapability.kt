/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.protocol

interface ObservableCapability<T> {
    var items: Iterable<T>
    fun onChange(key: Any, handler: () -> Unit) {}
    fun removeObserver(key: Any) {}
}
