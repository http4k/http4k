package org.http4k.core

/**
 * For support of both Synchronous and Asynchronous HTTP calls.
 */
interface AsyncHttpHandler {
    operator fun invoke(request: Request, fn: (Response) -> Unit)
}

/**
 * Convert a synchronous HttpHandler API to mimic Async API
 */
fun HttpHandler.withAsyncApi(): AsyncHttpHandler = object : AsyncHttpHandler, HttpHandler by this {
    override fun invoke(request: Request, fn: (Response) -> Unit) {
        fn(invoke(request))
    }
}