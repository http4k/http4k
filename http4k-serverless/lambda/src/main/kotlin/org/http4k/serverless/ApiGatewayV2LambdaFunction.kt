package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.queries
import org.http4k.core.then
import org.http4k.core.toUrlFormEncoded
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import org.http4k.serverless.aws.AwsGatewayProxyResponseV2
import java.io.InputStream
import java.io.OutputStream
import java.util.*

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
    override fun invoke(req: AwsGatewayProxyRequestV2): Request {
        val method = Method.valueOf(req.requestContext?.http?.method ?: error("method is missing"))
        val query = req.queryStringParameters?.toList() ?: Uri.of(req.rawQueryString.orEmpty()).queries()
        val uri = Uri.of(req.rawPath.orEmpty()).query(query.toUrlFormEncoded())
        val body = req.body?.let { MemoryBody(if (req.isBase64Encoded) Base64.getDecoder().decode(it.toByteArray()) else it.toByteArray()) } ?: Body.EMPTY
        val headers = (req.headers?.map { (k, v) -> v.split(",").map { k to it } }?.flatten() ?: emptyList()) +
            (req.cookies?.map { "Cookie" to it } ?: emptyList())
        return Request(method, uri).body(body).headers(headers)
    }

    override fun invoke(req: Response): AwsGatewayProxyResponseV2 {
        val nonCookies = req.headers.filterNot { it.first.toLowerCase() == "set-cookie" }
        return AwsGatewayProxyResponseV2(
            statusCode = req.status.code,
            multiValueHeaders = nonCookies.groupBy { it.first }.mapValues { it.value.map { it.second } }.toMap(),
            headers = nonCookies.toMap(),
            body = req.bodyString(),
            cookies = req.cookies().map(Cookie::fullCookieString)
        )
    }
}
