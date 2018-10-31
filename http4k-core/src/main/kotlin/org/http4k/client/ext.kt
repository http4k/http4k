package org.http4k.client

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import java.io.Closeable

interface AsyncHttpClient : Closeable {
    operator fun invoke(request: Request, fn: (Response) -> Unit)
    override fun close() {}
}

interface DualSyncAsyncHttpHandler: HttpHandler, AsyncHttpClient

/**
 * Convert a synchronous HttpHandler API to mimic AsyncHttpClient
 */
fun HttpHandler.withAsyncApi(): AsyncHttpClient = object : DualSyncAsyncHttpHandler {
    override fun invoke(p1: Request): Response = this@withAsyncApi(p1)

    override fun invoke(request: Request, fn: (Response) -> Unit) = fn(invoke(request))
}