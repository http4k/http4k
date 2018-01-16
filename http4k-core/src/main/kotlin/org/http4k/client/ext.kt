package org.http4k.client

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.Closeable
import java.lang.Exception

interface AsyncHttpClient : Closeable {
    operator fun invoke(request: Request, fn: (Response) -> Unit)
    override fun close() {}
}

/**
 * Convert a synchronous HttpHandler API to mimic AsyncHttpClient
 */
fun HttpHandler.withAsyncApi(): AsyncHttpClient = object : AsyncHttpClient, HttpHandler by this {
    override fun invoke(request: Request, fn: (Response) -> Unit) = fn(invoke(request))
}

fun Status.describeClientError(e: Exception) = description("Client error, caused by ${e.localizedMessage}")