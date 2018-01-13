package org.http4k.core

import java.io.Closeable

/**
 * For support of both Synchronous and Asynchronous HTTP calls.
 */
interface AsyncHttpHandler : Closeable {
    operator fun invoke(request: Request, fn: (Response) -> Unit)
    override fun close() {}
}

/**
 * Convert a synchronous HttpHandler API to mimic Async API
 */
fun HttpHandler.withAsyncApi(): AsyncHttpHandler = object : AsyncHttpHandler, HttpHandler by this {
    override fun invoke(request: Request, fn: (Response) -> Unit) {
        fn(invoke(request))
    }
}