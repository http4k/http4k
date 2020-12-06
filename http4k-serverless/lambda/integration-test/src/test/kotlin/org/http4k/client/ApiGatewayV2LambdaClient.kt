package org.http4k.client

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import org.http4k.aws.FunctionName
import org.http4k.aws.Region
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.queries
import org.http4k.format.Jackson.auto
import org.http4k.serverless.aws.AwsGatewayProxyRequestV2
import org.http4k.serverless.aws.Http
import org.http4k.serverless.aws.RequestContext

class ApiGatewayV2LambdaClient(functionName: FunctionName, region: Region) :
    LambdaHttpClient(functionName, region) {
    override fun inject(it: Request): (Request) -> Request = requestLens of AwsGatewayProxyRequestV2(requestContext = RequestContext(Http(it.method.name))).apply {
        rawPath = it.uri.path
        queryStringParameters = it.uri.queries().filterNot { it.second == null }.map { it.first to it.second!! }.toMap()
        body = it.bodyString()
        headers = it.headers.groupBy { it.first }.mapValues { (k, v) -> v.map { it.second }.joinToString(",") }
    }

    override fun extract(lambdaResponse: Response): Response {
        val response = responseLens(lambdaResponse)
        return Response(Status(response.statusCode, ""))
            .headers((response.headers
                ?: emptyMap()).entries.fold(listOf()) { acc, next -> acc + (next.key to next.value) })
            .headers((response.multiValueHeaders ?: emptyMap()).entries.fold(listOf(), { acc, next ->
                next.value.fold(acc, { acc2, next2 -> acc2 + (next.key to next2) })
            }))
            .headers((response.cookies ?: emptyList()).fold(listOf(), { acc, next -> acc + ("set-cookie" to next) }))
            .body(response.body.orEmpty())
    }

    private val requestLens = Body.auto<AwsGatewayProxyRequestV2>().toLens()
    private val responseLens = Body.auto<APIGatewayV2HTTPResponse>().toLens()

}
