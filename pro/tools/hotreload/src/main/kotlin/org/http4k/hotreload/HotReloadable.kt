/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.hotreload

/**
 * Base interface for creating a hot-reloadable app. Note that
 * these are required to be top-level classes as they are instantiated via
 * reflection on hot-reload. Thus, they cannot require constructor parameters.
 */
interface HotReloadable<T> {
    fun create(): T
}
