package org.http4k.connect.example

import org.http4k.core.HttpHandler

fun Example.Companion.Http(httpHandler: HttpHandler) = object : Example {
    override suspend fun <R> invoke(action: ExampleAction<R>) = action.toResult(httpHandler(action.toRequest()))
}
