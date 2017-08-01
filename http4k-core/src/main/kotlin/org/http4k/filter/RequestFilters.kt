package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Request

object RequestFilters {

    /**
     * Intercept the request before it is sent to the next service.
     */
    fun Tap(fn: (Request) -> Unit) = Filter { next ->
        {
            fn(it)
            next(it)
        }
    }

    /**
     * Basic GZipping of Request. Does not currently support GZipping streams
     */
    fun GZip(): Filter = Filter { next ->
        { next(it.body(it.body.gzipped())) }
    }

    /**
     * Basic UnGZipping of Request. Does not currently support GZipping streams
     */
    fun GunZip(): Filter = Filter { next ->
        { next(it.body(it.body.gunzipped())) }
    }
}

