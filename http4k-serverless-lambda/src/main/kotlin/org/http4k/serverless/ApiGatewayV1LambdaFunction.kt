package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.toUrlFormEncoded

/**
 * This is the main entry point for lambda invocations using the V1 payload format.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 */
abstract class ApiGatewayV1LambdaFunction(appLoader: AppLoaderWithContexts)
    : AwsLambdaFunction<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>(ApiGatewayV1AwsHttpAdapter, appLoader) {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    override fun handleRequest(req: APIGatewayProxyRequestEvent, ctx: Context) = handle(req, ctx)
}

internal object ApiGatewayV1AwsHttpAdapter : AwsHttpAdapter<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    override fun invoke(req: APIGatewayProxyRequestEvent) = (req.headers ?: emptyMap()).toList().fold(
        Request(Method.valueOf(req.httpMethod), req.uri())
            .body(req.body?.let(::MemoryBody) ?: Body.EMPTY)) { memo, (first, second) ->
        memo.header(first, second)
    }

    override fun invoke(req: Response) = APIGatewayProxyResponseEvent().also {
        it.statusCode = req.status.code
        it.headers = req.headers.toMap()
        it.body = req.bodyString()
    }

    private fun APIGatewayProxyRequestEvent.uri() = Uri.of(path ?: "").query((queryStringParameters
        ?: emptyMap()).toList().toUrlFormEncoded())
}
