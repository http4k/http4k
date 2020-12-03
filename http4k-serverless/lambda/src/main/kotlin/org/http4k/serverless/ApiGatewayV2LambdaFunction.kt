package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.queries
import org.http4k.core.toUrlFormEncoded
import java.util.Base64

/**
 * This is the main entry point for lambda invocations using the V2 payload format.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 */
abstract class ApiGatewayV2LambdaFunction(appLoader: AppLoaderWithContexts)
    : AwsLambdaFunction<Map<String, Any>, Map<String, Any>>(ApiGatewayV2AwsHttpAdapter, appLoader) {
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
        val uri = getString("rawPath").orEmpty()
        val body = getString("body")
            ?.let {
                MemoryBody(when {
                    getBoolean("isBase64Encoded") == true -> Base64.getDecoder().decode(it.toByteArray())
                    else -> it.toByteArray()
                })
            }
            ?: Body.EMPTY
        val headers = (getStringMap("headers")
            ?.map { (k, v) -> v.split(",").map { k to it } }?.flatten()
            ?: emptyList()) +
            (getStringList("cookies")?.map { "Cookie" to it } ?: emptyList())

        return Request(Method.valueOf(method), Uri.of(uri).query(query.toUrlFormEncoded())).body(body).headers(headers)
    }

    override fun invoke(req: Map<String, Any>, ctx: Context): Request = req.toHttp4kRequest()

    override fun invoke(req: Response): Map<String, Any> {
        val nonCookies = req.headers.filterNot { it.first.toLowerCase() == "set-cookie" }
        return mapOf(
            "statusCode" to req.status.code,
            "headers" to nonCookies.toMap(),
            "multiValueHeaders" to nonCookies.groupBy { it.first }.mapValues { it.value.map { it.second } }.toMap(),
            "cookies" to req.cookies().map(Cookie::fullCookieString),
            "body" to req.bodyString()
        )
    }

    fun Map<*, *>.getNested(name: String): Map<*, *>? = get(name) as? Map<*, *>
    fun Map<*, *>.getString(name: String): String? = get(name) as? String
    fun Map<*, *>.getBoolean(name: String): Boolean? = get(name) as? Boolean

    @Suppress("UNCHECKED_CAST")
    fun Map<*, *>.getStringMap(name: String): Map<String, String>? = get(name) as? Map<String, String>

    @Suppress("UNCHECKED_CAST")
    fun Map<*, *>.getStringList(name: String): List<String>? = get(name) as? List<String>
}
