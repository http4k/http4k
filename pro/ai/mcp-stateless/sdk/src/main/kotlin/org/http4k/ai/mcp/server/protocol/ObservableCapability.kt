/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

/**
 * A capability whose list of items can change at runtime. A `subscriptions/listen` stream registers an
 * observer (keyed by that stream) to be told when [items] is reassigned, and removes it when the stream
 * closes. Default no-ops so non-observable implementations (e.g. directory-backed) need no boilerplate.
 */
interface ObservableCapability<T> {
    var items: Iterable<T>
    fun onChange(key: Any, handler: () -> Unit) {}
    fun removeObserver(key: Any) {}
}
