package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.core.HttpHandler
import org.http4k.core.Response

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
    override fun invoke(req: APIGatewayProxyRequestEvent) =
        RequestContent(req.path, req.queryStringParameters, null, req.body, req.isBase64Encoded, req.httpMethod, req.headers, emptyList<String>()).asHttp4k()

    override fun invoke(req: Response) = APIGatewayProxyResponseEvent().also {
        it.statusCode = req.status.code
        it.headers = req.headers.toMap()
        it.body = req.bodyString()
    }
}

