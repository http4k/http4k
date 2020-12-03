package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import org.http4k.serverless.aws.AwsGatewayProxyResponseV2
import org.http4k.serverless.aws.toAwsResponse
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
        val req = inputStream.use { Jackson.asA<AwsGatewayProxyRequestV2>(String(it.readAllBytes())) }
        val res = ApiGatewayV2AwsHttpAdapter(ServerFilters.InitialiseRequestContext(contexts).then(AddLambdaContextAndRequest(ctx, req, contexts)).then(app)(ApiGatewayV2AwsHttpAdapter(req)))
        outputStream.use { it.write(Jackson.asFormatString(res).toByteArray()) }
    }
}

object ApiGatewayV2AwsHttpAdapter : AwsHttpAdapter<AwsGatewayProxyRequestV2, AwsGatewayProxyResponseV2> {
    override fun invoke(req: AwsGatewayProxyRequestV2): Request = req.toHttp4kRequest()

    override fun invoke(res: Response) = res.toAwsResponse()
}
