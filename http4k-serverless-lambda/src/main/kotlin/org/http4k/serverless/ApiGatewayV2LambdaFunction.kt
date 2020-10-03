package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method.valueOf
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.toUrlFormEncoded

/**
 * This is the main entry point for lambda invocations using the V2 payload format.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 */
abstract class ApiGatewayV2LambdaFunction(appLoader: AppLoaderWithContexts)
    : AwsLambdaFunction<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>(ApiGatewayV2AwsHttpAdapter, appLoader) {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    override fun handleRequest(req: APIGatewayV2HTTPEvent, ctx: Context) = handle(req, ctx)
}

internal object ApiGatewayV2AwsHttpAdapter : AwsHttpAdapter<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    override fun invoke(req: APIGatewayV2HTTPEvent) = (req.headers ?: emptyMap()).toList().fold(
        Request(valueOf(req.requestContext.http.method), req.uri())
            .body(req.body?.let(::MemoryBody) ?: Body.EMPTY)) { memo, (first, second) ->
        memo.header(first, second)
    }

    override fun invoke(req: Response) = APIGatewayV2HTTPResponse().also {
        it.statusCode = req.status.code
        it.multiValueHeaders = req.headers.groupBy { it.first }.mapValues { it.value.map { it.second } }.toMap()
        it.body = req.bodyString()
    }

    private fun APIGatewayV2HTTPEvent.uri() = Uri.of(rawPath ?: "").query((queryStringParameters
        ?: emptyMap()).toList().toUrlFormEncoded())
}
