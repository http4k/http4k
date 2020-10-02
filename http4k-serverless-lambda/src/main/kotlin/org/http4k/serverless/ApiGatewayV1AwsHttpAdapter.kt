package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.core.Body
import org.http4k.core.MemoryBody
import org.http4k.core.Method.valueOf
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.toUrlFormEncoded

object ApiGatewayV1AwsHttpAdapter : AwsHttpAdapter<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    override fun invoke(req: APIGatewayProxyRequestEvent) = (req.headers ?: emptyMap()).toList().fold(
        Request(valueOf(req.httpMethod), req.uri())
            .body(req.body?.let(::MemoryBody) ?: Body.EMPTY)) { memo, (first, second) ->
        memo.header(first, second)
    }

    override fun invoke(req: Response) = APIGatewayProxyResponseEvent().also {
        it.statusCode = req.status.code
        it.headers = req.headers.toMap()
        it.body = req.bodyString()
    }

    private fun APIGatewayProxyRequestEvent.uri() = Uri.of(path ?: "").query((queryStringParameters
        ?: emptyMap()).toList().toUrlFormEncoded())
}
