package org.http4k.serverless.lambda

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.toUrlFormEncoded
import org.http4k.filter.ServerFilters
import org.http4k.serverless.BootstrapAppLoader

const val LAMBDA_CONTEXT_KEY = "HTTP4K_LAMBDA_CONTEXT"

/**
 * This is the main entry point for the lambda. It uses the local environment
 * to instantiate the Http4k handler which can be used for further invocations.
 */
class LambdaFunction(env: Map<String, String> = System.getenv()) {
    private val contexts = RequestContexts()
    private val app = BootstrapAppLoader(env, contexts)
    private val initializeRequestContext = ServerFilters.InitialiseRequestContext(contexts)

    fun handle(request: ApiGatewayProxyRequest, lambdaContext: Context? = null) =
        initializeRequestContext
            .then(AddLambdaContext(lambdaContext, contexts))
            .then(app)
            .invoke(request.asHttp4k())
            .asApiGateway()
}

internal fun Response.asApiGateway() = ApiGatewayProxyResponse(status.code, headers.toMap(), bodyString())

internal fun ApiGatewayProxyRequest.asHttp4k() = (headers ?: emptyMap()).toList().fold(
    Request(Method.valueOf(httpMethod), uri())
        .body(body?.let(::MemoryBody) ?: Body.EMPTY)) { memo, (first, second) ->
    memo.header(first, second)
}

internal fun ApiGatewayProxyRequest.uri() = Uri.of(path ?: "").query((queryStringParameters
    ?: emptyMap()).toList().toUrlFormEncoded())

internal fun AddLambdaContext(lambdaContext: Context?, contexts: RequestContexts) = Filter { next ->
    {
        if (lambdaContext != null) {
            contexts[it][LAMBDA_CONTEXT_KEY] = lambdaContext
        }
        next(it)
    }
}
