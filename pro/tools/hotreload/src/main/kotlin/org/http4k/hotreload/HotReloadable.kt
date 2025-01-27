package org.http4k.hotreload

/**
 * Base interface for creating a hot-reloadable app. Note that
 * these are required to be top-level classes as they are instantiated via
 * reflection on hot-reload. Thus, they cannot require constructor parameters.
 */
interface HotReloadable<T> {
    fun create(): T
}
