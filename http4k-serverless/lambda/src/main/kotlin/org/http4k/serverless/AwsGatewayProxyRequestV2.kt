package org.http4k.serverless

import org.http4k.core.Body
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.queries
import org.http4k.core.toUrlFormEncoded
import java.util.*

data class AwsGatewayProxyRequestV2(var version: String? = null,
                                    var routeKey: String? = null,
                                    var rawPath: String? = null,
                                    var rawQueryString: String? = null,
                                    var cookies: List<String>? = null,
                                    var headers: Map<String, String>? = null,
                                    var queryStringParameters: Map<String, String>? = null,
                                    var pathParameters: Map<String, String>? = null,
                                    var stageVariables: Map<String, String>? = null,
                                    var body: String? = null,
                                    var isBase64Encoded: Boolean = false,
                                    val requestContext: RequestContext? = null)

data class RequestContext(var http: Http? = null)

data class Http(var method: String? = null)

fun AwsGatewayProxyRequestV2.toHttp4kRequest() = Request(http4kMethod(), uri()).body(body()).headers(http4kHeaders())

private fun AwsGatewayProxyRequestV2.http4kHeaders(): Parameters =
    (headers?.map { (k, v) -> v.split(",").map { k to it } }?.flatten() ?: emptyList()) +
        (cookies?.map { "Cookie" to it } ?: emptyList())

private fun AwsGatewayProxyRequestV2.http4kMethod(): Method = Method.valueOf(requestContext?.http?.method ?: "HEAD")

private fun AwsGatewayProxyRequestV2.uri(): Uri {
    val query = queryStringParameters?.toList() ?: Uri.of(rawQueryString.orEmpty()).queries()
    return Uri.of(rawPath.orEmpty()).query(query.toUrlFormEncoded())
}

private fun AwsGatewayProxyRequestV2.body(): Body =
    body?.let { MemoryBody(if (isBase64Encoded) Base64.getDecoder().decode(it.toByteArray()) else it.toByteArray()) }
        ?: Body.EMPTY
