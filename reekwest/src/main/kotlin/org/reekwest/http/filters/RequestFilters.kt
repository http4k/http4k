package org.reekwest.http.filters

import org.reekwest.http.core.Filter
import org.reekwest.http.core.Request

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

