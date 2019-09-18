package org.http4k.serverless.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
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
const val LAMBDA_REQUEST_KEY = "HTTP4K_LAMBDA_REQUEST"

/**
 * This is the main entry point for the lambda. It uses the local environment
 * to instantiate the Http4k handler which can be used for further invocations.
 */
class LambdaFunction(env: Map<String, String> = System.getenv()) {
    private val contexts = RequestContexts()
    private val app = BootstrapAppLoader(env, contexts)
    private val initializeRequestContext = ServerFilters.InitialiseRequestContext(contexts)

    fun handle(request: APIGatewayProxyRequestEvent, lambdaContext: Context? = null) =
        initializeRequestContext
            .then(AddLambdaContextAndRequest(lambdaContext, request, contexts))
            .then(app)(request.asHttp4k())
            .asApiGateway()
}

internal fun Response.asApiGateway(): APIGatewayProxyResponseEvent {
    val apiGatewayResponse = APIGatewayProxyResponseEvent()

    apiGatewayResponse.statusCode = status.code
    apiGatewayResponse.headers = headers.toMap()
    apiGatewayResponse.body = bodyString()

    return apiGatewayResponse
}

internal fun APIGatewayProxyRequestEvent.asHttp4k() = (headers ?: emptyMap()).toList().fold(
    Request(Method.valueOf(httpMethod), uri())
        .body(body?.let(::MemoryBody) ?: Body.EMPTY)) { memo, (first, second) ->
    memo.header(first, second)
}

internal fun APIGatewayProxyRequestEvent.uri() = Uri.of(path ?: "").query((queryStringParameters
    ?: emptyMap()).toList().toUrlFormEncoded())

internal fun AddLambdaContextAndRequest(lambdaContext: Context?, request: APIGatewayProxyRequestEvent, contexts: RequestContexts) = Filter { next ->
    {
        lambdaContext?.apply { contexts[it][LAMBDA_CONTEXT_KEY] = lambdaContext }
        contexts[it][LAMBDA_REQUEST_KEY] = request
        next(it)
    }
}
