package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED

class TestAppWithContexts<Req>(private val getAccountId: (Req) -> String) : AppLoaderWithContexts {
    override fun invoke(env: Map<String, String>, contexts: RequestContexts): HttpHandler = LamdbdaTestAppWithContext(env, contexts, getAccountId)
}

fun <Req> LamdbdaTestAppWithContext(env: Map<String, String>, contexts: RequestContexts, getAccountId: (Req) -> String): HttpHandler = { request ->
    val lambdaContext: Context? = contexts[request][LAMBDA_CONTEXT_KEY]
    val lambdaRequest: Req = contexts[request][LAMBDA_REQUEST_KEY]!!

    env.toList().fold(Response(CREATED)) { memo, (key, value) ->
        memo.header(key, value)
    }.body(
        request
            .header("LAMBDA_CONTEXT_FUNCTION_NAME", lambdaContext?.functionName)
            .header("LAMBDA_REQUEST_VALUE", getAccountId(lambdaRequest))
            .removeHeader("x-http4k-context").toString()
    )
}
