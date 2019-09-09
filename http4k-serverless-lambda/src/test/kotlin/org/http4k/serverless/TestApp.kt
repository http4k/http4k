package org.http4k.serverless

import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED

object TestApp : AppLoader {
    override fun invoke(env: Map<String, String>): HttpHandler = { request ->
        env.toList().fold(Response(CREATED)) { memo, (key, value) ->
            memo.header(key, value)
        }.body(request.toString())
    }
}

object TestAppWithContexts : AppLoaderWithContexts {
    override fun invoke(env: Map<String, String>, contexts: RequestContexts): HttpHandler = { request ->
        env.toList().fold(Response(CREATED)) { memo, (key, value) ->
            memo.header(key, value)
        }.body(request.toString())
    }
}
