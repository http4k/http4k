package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Request

object RequestFilters {

    /**
     * Intercept the request before it is sent to the next service.
     */
    object Tap {
        operator fun invoke(fn: (Request) -> Unit) = Filter { next ->
            {
                fn(it)
                next(it)
            }
        }
    }

    /**
     * Basic GZipping of Request. Does not currently support GZipping streams
     */
    object GZip {
        operator fun invoke() = Filter { next ->
            { request ->
                next(request.body(request.body.gzipped()).replaceHeader("content-encoding", "gzip"))
            }
        }
    }

    /**
     * Basic UnGZipping of Request. Does not currently support GZipping streams
     */
    object GunZip {
        operator fun invoke() = Filter { next ->
            { request ->
                request.header("content-encoding")
                    ?.let { if (it.contains("gzip")) it else null }
                    ?.let { next(request.body(request.body.gunzipped())) } ?: next(request)
            }
        }
    }

}

