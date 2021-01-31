package org.http4k.client

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import org.http4k.aws.Function
import org.http4k.aws.Region
import org.http4k.base64Decoded
import org.http4k.base64Encode
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.queries
import org.http4k.format.Jackson.auto
import org.http4k.serverless.aws.AwsGatewayProxyRequestV2
import org.http4k.serverless.aws.Http
import org.http4k.serverless.aws.RequestContext

class ApiGatewayV2LambdaClient(function: Function, region: Region) : LambdaHttpClient(function, region) {
    override fun Request.toLambdaFormat(): (Request) -> Request = requestLens of AwsGatewayProxyRequestV2(requestContext = RequestContext(Http(method.name))).apply {
        rawPath = uri.path
        queryStringParameters = uri.queries().filterNot { it.second == null }.map { it.first to it.second!! }.toMap()
        body = bodyString().base64Encode()
        isBase64Encoded = true
        headers = this@toLambdaFormat.headers.groupBy { it.first }.mapValues { (_, v) -> v.map { it.second }.joinToString(",") }
    }

    override fun Response.fromLambdaFormat(): Response {
        val response = responseLens(this)
        return Response(Status(response.statusCode, ""))
            .headers((response.headers
                ?: emptyMap()).entries.fold(listOf()) { acc, next -> acc + (next.key to next.value) })
            .headers((response.multiValueHeaders ?: emptyMap()).entries.fold(listOf(), { acc, next ->
                next.value.fold(acc, { acc2, next2 -> acc2 + (next.key to next2) })
            }))
            .headers((response.cookies ?: emptyList()).fold(listOf(), { acc, next -> acc + ("set-cookie" to next) }))
            .body((if (response.isBase64Encoded) response.body?.base64Decoded() else response.body) ?: "")
    }

    private val requestLens = Body.auto<AwsGatewayProxyRequestV2>().toLens()
    private val responseLens = Body.auto<APIGatewayV2HTTPResponse>().toLens()

}
