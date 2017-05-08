package org.http4k.filters

import org.http4k.core.Filter
import org.http4k.core.Request

object RequestFilters {

    /**
     * Intercept the request before it is sent to the next service.
     */
    fun Tap(fn: (Request) -> Unit) = Filter {
        next ->
        {
            fn(it)
            next(it)
        }
    }
}

