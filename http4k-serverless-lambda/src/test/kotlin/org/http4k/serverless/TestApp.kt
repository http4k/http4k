package org.http4k.serverless

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status

object TestApp : AppLoader {
    override fun invoke(env: Map<String, String>): HttpHandler = { request ->
        env.toList().fold(Response(Status.CREATED)) { memo, (key, value) ->
            memo.header(key, value)
        }.body(request.removeHeader("x-http4k-context").toString())
    }
}
