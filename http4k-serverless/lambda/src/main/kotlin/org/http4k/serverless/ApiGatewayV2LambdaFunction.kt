package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import org.http4k.base64Encode
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.queries
import org.http4k.core.toUrlFormEncoded

/**
 * This is the main entry point for lambda invocations using the V2 payload format.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 */
abstract class ApiGatewayV2LambdaFunction(appLoader: AppLoaderWithContexts)
    : AwsLambdaFunction<Map<String, Any>, Map<String, Any>>(ApiGatewayV2AwsHttpAdapter, appLoader),
    RequestHandler<Map<String, Any>, Map<String, Any>> {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    override fun handleRequest(req: Map<String, Any>, ctx: Context) = handle(req, ctx)
}

object ApiGatewayV2AwsHttpAdapter : AwsHttpAdapter<Map<String, Any>, Map<String, Any>> {
    private fun Map<String, Any>.toHttp4kRequest(): Request {
        val method = (getNested("requestContext")?.getNested("http")?.get("method") as? String)
            ?: error("method is invalid")
        val query: Parameters = getStringMap("queryStringParameters")
            ?.toList()
            ?: Uri.of(getString("rawQueryString").orEmpty()).queries()
        val headers = toHeaders() +
            (getStringList("cookies")?.map { "Cookie" to it } ?: emptyList())

        return Request(Method.valueOf(method),
            Uri.of(getString("rawPath").orEmpty())
                .query(query.toUrlFormEncoded()))
            .headers(headers)
            .body(toBody())
    }

    override fun invoke(req: Map<String, Any>, ctx: Context): Request = req.toHttp4kRequest()

    override fun invoke(resp: Response): Map<String, Any> {
        val nonCookies = resp.headers.filterNot { it.first.toLowerCase() == "set-cookie" }
        return mapOf(
            "statusCode" to resp.status.code,
            "headers" to nonCookies.toMap(),
            "multiValueHeaders" to nonCookies.groupBy { it.first }.mapValues { it.value.map { it.second } }.toMap(),
            "cookies" to resp.cookies().map(Cookie::fullCookieString),
            "body" to resp.bodyString().base64Encode(),
            "isBase64Encoded" to true
        )
    }
}
