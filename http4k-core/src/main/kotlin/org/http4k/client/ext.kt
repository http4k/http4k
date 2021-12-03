package org.http4k.client

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.Closeable

interface AsyncHttpHandler : Closeable {
    operator fun invoke(request: Request, fn: (Response) -> Unit)
    override fun close() {}
}

interface DualSyncAsyncHttpHandler : HttpHandler, AsyncHttpHandler

/**
 * Convert a synchronous HttpHandler API to mimic AsyncHttpClient
 */
fun HttpHandler.withAsyncApi(): AsyncHttpHandler = object : DualSyncAsyncHttpHandler {
    override fun invoke(p1: Request): Response = this@withAsyncApi(p1)

    override fun invoke(request: Request, fn: (Response) -> Unit) = fn(invoke(request))
}

fun Status.toClientStatus(e: Exception) = description("Client Error: $description caused by ${e.localizedMessage}")
