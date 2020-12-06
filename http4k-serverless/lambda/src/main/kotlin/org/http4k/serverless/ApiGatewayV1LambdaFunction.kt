package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.http4k.base64Encode
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response

/**
 * This is the main entry point for lambda invocations using the V1 payload format.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 */
abstract class ApiGatewayV1LambdaFunction(appLoader: AppLoaderWithContexts)
    : AwsLambdaFunction<APIGatewayProxyRequestEvent, Map<String, Any>>(ApiGatewayV1AwsHttpAdapter, appLoader), RequestHandler<APIGatewayProxyRequestEvent, Map<String, Any>> {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    override fun handleRequest(req: APIGatewayProxyRequestEvent, ctx: Context) = handle(req, ctx)
}

object ApiGatewayV1AwsHttpAdapter : AwsHttpAdapter<APIGatewayProxyRequestEvent, Map<String, Any>> {
    override fun invoke(req: APIGatewayProxyRequestEvent, ctx: Context): Request {
        return RequestContent(req.path, req.queryStringParameters, null, req.body, req.isBase64Encoded, req.httpMethod, (req.headers
            ?: emptyMap()).mapValues { listOf(it.value) }, emptyList()).asHttp4k()
    }

    override fun invoke(resp: Response) = mapOf(
        "statusCode" to resp.status.code,
        "headers" to resp.headers.toMap(),
        "body" to resp.bodyString().base64Encode(),
        "isBase64Encoded" to true
    )
}

