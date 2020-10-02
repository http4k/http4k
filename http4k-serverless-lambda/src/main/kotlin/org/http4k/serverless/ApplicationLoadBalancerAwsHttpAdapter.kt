package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent
import org.http4k.core.Body
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.toUrlFormEncoded

object ApplicationLoadBalancerAwsHttpAdapter : AwsHttpAdapter<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {
    override fun invoke(req: ApplicationLoadBalancerRequestEvent) = (req.headers ?: emptyMap()).toList().fold(
        Request(Method.valueOf(req.httpMethod), req.uri())
            .body(req.body?.let(::MemoryBody) ?: Body.EMPTY)) { memo, (first, second) ->
        memo.header(first, second)
    }

    override fun invoke(req: Response) = ApplicationLoadBalancerResponseEvent().also {
        it.statusCode = req.status.code
        it.headers = req.headers.toMap()
        it.body = req.bodyString()
    }

    private fun ApplicationLoadBalancerRequestEvent.uri() = Uri.of(path ?: "").query((queryStringParameters
        ?: emptyMap()).toList().toUrlFormEncoded())
}
