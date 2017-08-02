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
        { request ->
            val existingTransferEncodingHeader = request.header("transfer-encoding")?.let { ", " } ?: ""
            next(request.body(request.body.gzipped()).replaceHeader("transfer-encoding", existingTransferEncodingHeader + "gzip"))
        }
    }

    /**
     * Basic UnGZipping of Request. Does not currently support GZipping streams
     */
    fun GunZip() = Filter { next ->
        { request ->
            request.header("transfer-encoding")
                ?.let { if (it.contains("gzip")) it else null }
                ?.let { next(request.body(request.body.gunzipped())) } ?: next(request)
        }
    }

}

