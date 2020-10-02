package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import org.http4k.core.Body
import org.http4k.core.MemoryBody
import org.http4k.core.Method.valueOf
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.toUrlFormEncoded

object ApiGatewayV2AwsHttpAdapter : AwsHttpAdapter<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    override fun invoke(req: APIGatewayV2HTTPEvent) = (req.headers ?: emptyMap()).toList().fold(
        Request(valueOf(req.requestContext.http.method), req.uri())
            .body(req.body?.let(::MemoryBody) ?: Body.EMPTY)) { memo, (first, second) ->
        memo.header(first, second)
    }

    override fun invoke(req: Response) = APIGatewayV2HTTPResponse().also {
        it.statusCode = req.status.code
        it.multiValueHeaders = req.headers.groupBy { it.first }.mapValues { it.value.map { it.second } }.toMap()
        it.body = req.bodyString()
    }

    private fun APIGatewayV2HTTPEvent.uri() = Uri.of(rawPath ?: "").query((queryStringParameters
        ?: emptyMap()).toList().toUrlFormEncoded())
}
