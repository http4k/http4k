package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import java.io.InputStream
import java.io.OutputStream

/**
 * This is the main entry point for lambda invocations using the V2 payload format.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 */
abstract class ApiGatewayV2LambdaFunction(appLoader: AppLoaderWithContexts) : RequestStreamHandler {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val contexts = RequestContexts()
    private val app = appLoader(System.getenv(), contexts)

    override fun handleRequest(inputStream: InputStream, outputStream: OutputStream, ctx: Context) {
        val req = Jackson.asA<APIGatewayV2HTTPEvent>(String(inputStream.readAllBytes()))
        val res = ApiGatewayV2AwsHttpAdapter(ServerFilters.InitialiseRequestContext(contexts).then(AddLambdaContextAndRequest(ctx, req, contexts)).then(app)(ApiGatewayV2AwsHttpAdapter(req)))
        outputStream.write(Jackson.asFormatString(res).toByteArray())
    }
}

object ApiGatewayV2AwsHttpAdapter : AwsHttpAdapter<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    override fun invoke(req: APIGatewayV2HTTPEvent) =
        RequestContent(req.rawPath.orEmpty(), req.queryStringParameters, req.rawQueryString, req.body, req.isBase64Encoded, req.requestContext?.http?.method
            ?: "HEAD", req.headers, req.cookies ?: emptyList()).asHttp4k()

    override fun invoke(req: Response) = APIGatewayV2HTTPResponse().also {
        it.statusCode = req.status.code
        it.multiValueHeaders = req.headers.groupBy { it.first }.mapValues { it.value.map { it.second } }.toMap()
        it.headers = req.headers.toMap()
        it.body = req.bodyString()
    }
}
