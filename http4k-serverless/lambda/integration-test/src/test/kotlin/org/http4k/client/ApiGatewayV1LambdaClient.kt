package org.http4k.client

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.aws.Function
import org.http4k.aws.Region
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.toParameters
import org.http4k.format.Jackson.auto

class ApiGatewayV1LambdaClient(function: Function, region: Region) : LambdaHttpClient(function, region) {
    override fun inject(it: Request): (Request) -> Request = requestLens of APIGatewayProxyRequestEvent()
        .withHttpMethod(it.method.name)
        .withHeaders(it.headers.toMap())
        .withPath(it.uri.path)
        .withQueryStringParameters(it.uri.query.toParameters().toMap())
        .withBody(it.bodyString())

    override fun extract(lambdaResponse: Response): Response {
        val response = responseLens(lambdaResponse)
        return Response(Status(response.statusCode, ""))
            .headers(response.headers.map { kv -> kv.toPair() })
            .body(response.body)
    }

    private val requestLens = Body.auto<APIGatewayProxyRequestEvent>().toLens()
    private val responseLens = Body.auto<APIGatewayProxyResponseEvent>().toLens()

}
