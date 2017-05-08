package org.http4k.http.filters

import org.http4k.http.core.Filter
import org.http4k.http.core.Request

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

