package org.http4k.hotreload

import org.http4k.core.HttpHandler
import org.http4k.server.PolyHandler

/**
 * Base interface for creating a hot-reloadable app. Note that
 * these are required to be top-level classes as they are instantiated via
 * reflection on hot-reload. Thus, they cannot require constructor parameters.
 */
interface HotReloadable {

    /**
     * Implement this to provide a hot-reloading HttpHandler
     */
    interface Http {
        fun create(): HttpHandler
    }

    /**
     * Implement this to provide a hot-reloading PolyHandler
     */
    interface Poly {
        fun create(): PolyHandler
    }
}
