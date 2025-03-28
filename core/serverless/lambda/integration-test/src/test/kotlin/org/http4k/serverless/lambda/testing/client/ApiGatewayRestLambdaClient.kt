package org.http4k.serverless.lambda.testing.client

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.base64Decoded
import org.http4k.base64Encode
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.lambda.model.Function
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.core.toParameters
import org.http4k.format.Jackson.auto

class ApiGatewayRestLambdaClient(function: Function, region: Region) : LambdaHttpClient(function, region) {
    override fun Request.toLambdaFormat(): (Request) -> Request = requestLens of APIGatewayProxyRequestEvent()
        .withHttpMethod(method.name)
        .withHeaders(headers.toMap())
        .withPath(uri.path)
        .withQueryStringParameters(uri.query.toParameters().toMap())
        .withBody(body.payload.base64Encode())
        .withIsBase64Encoded(true)

    override fun Response.fromLambdaFormat(): Response {
        if (!status.successful) {
            return Response(BAD_GATEWAY).body(bodyString())
        }
        val response = responseLens(this)
        return Response(Status(response.statusCode, ""))
            .headers(response.headers.map { kv -> kv.toPair() })
            .body((if (response.isBase64Encoded == true) response.body?.base64Decoded() else response.body) ?: "")
    }

    private val requestLens = Body.auto<APIGatewayProxyRequestEvent>().toLens()
    private val responseLens = Body.auto<APIGatewayProxyResponseEvent>().toLens()
}
