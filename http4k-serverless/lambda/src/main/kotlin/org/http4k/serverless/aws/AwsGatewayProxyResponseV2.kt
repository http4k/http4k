package org.http4k.serverless.aws

import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies

data class AwsGatewayProxyResponseV2(
    val statusCode: Int = 0,
    val headers: Map<String, String?> = mapOf(),
    val multiValueHeaders: Map<String, List<String?>> = mapOf(),
    val cookies: List<String> = listOf(),
    val body: String? = null,
    val isBase64Encoded: Boolean = false
)

fun Response.toAwsResponse(): AwsGatewayProxyResponseV2 {
    val nonCookies = headers.filterNot { it.first.toLowerCase() == "set-cookie" }
    return AwsGatewayProxyResponseV2(
        statusCode = status.code,
        multiValueHeaders = nonCookies.groupBy { it.first }.mapValues { it.value.map { it.second } }.toMap(),
        headers = nonCookies.toMap(),
        body = this.bodyString(),
        cookies = this.cookies().map(Cookie::fullCookieString)
    )
}
