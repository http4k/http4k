package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED

object TestAppWithContexts : AppLoaderWithContexts {
    override fun invoke(env: Map<String, String>, contexts: RequestContexts): HttpHandler = { request ->
        val lambdaContext: Context? = contexts[request][LAMBDA_CONTEXT_KEY]
        val lambdaRequest: APIGatewayProxyRequestEvent? = contexts[request][LAMBDA_REQUEST_KEY]

        env.toList().fold(Response(CREATED)) { memo, (key, value) ->
            memo.header(key, value)
        }.body(
            request
                .header("LAMBDA_CONTEXT_FUNCTION_NAME", lambdaContext?.functionName)
                .header("LAMBDA_REQUEST_ACCOUNT_ID", lambdaRequest?.requestContext?.accountId)
                .removeHeader("x-http4k-context").toString()
        )
    }
}
