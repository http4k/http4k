package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.core.*
import org.http4k.filter.ServerFilters.InitialiseRequestContext

const val LAMBDA_CONTEXT_KEY = "HTTP4K_LAMBDA_CONTEXT"
const val LAMBDA_REQUEST_KEY = "HTTP4K_LAMBDA_REQUEST"

/**
 * This is the main entry point for the lambda. It uses the local environment
 * to instantiate the Http4k handler which can be used for further invocations.
 */
open class LambdaFunction(appLoader: AppLoaderWithContexts) {
    constructor(input: AppLoader) : this(object : AppLoaderWithContexts {
        override fun invoke(env: Map<String, String>, contexts: RequestContexts) = input(env)
    })

    constructor(input: HttpHandler) : this(object : AppLoader {
        override fun invoke(env: Map<String, String>): HttpHandler = input
    })

    @Deprecated("This reflection based implementation will be removed in future versions")
    constructor(env: Map<String, String> = System.getenv()) : this(object : AppLoaderWithContexts {
        override fun invoke(otherEnv: Map<String, String>, contexts: RequestContexts) = BootstrapAppLoader(env, contexts)
    })

    private val contexts = RequestContexts()
    private val app = appLoader(System.getenv(), contexts)

    fun handle(request: APIGatewayProxyRequestEvent, lambdaContext: Context? = null) =
        InitialiseRequestContext(contexts)
            .then(AddLambdaContextAndRequest(lambdaContext, request, contexts))
            .then(app)(request.asHttp4k())
            .asApiGateway()
}

internal fun Response.asApiGateway() = APIGatewayProxyResponseEvent().also {
    it.statusCode = status.code
    it.headers = headers.toMap()
    it.body = bodyString()
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
