package org.http4k.serverless.lambda.client

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.aws.FunctionName
import org.http4k.aws.Region
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.toParameters
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.lens.BiDiBodyLens

class LambdaHttpClient(functionName: FunctionName, region: Region):Filter {
    private fun callFunction(functionName: FunctionName) = Filter { next ->
        {
            val adapter = AwsClientV1HttpAdapter()
            val lambdaResponse = next(
                Request(Method.POST, "/2015-03-31/functions/${functionName.value}/invocations")
                    .header("X-Amz-Invocation-Type", "RequestResponse")
                    .header("X-Amz-Log-Type", "Tail")
                    .with(adapter.requestLens of adapter(it))
            )

            val response = adapter.responseLens(lambdaResponse)

            adapter(response)
        }
    }

    private val filter = callFunction(functionName).then(LambdaApi(region))

    override fun invoke(handler: HttpHandler): HttpHandler = filter(handler)
}

object LambdaApi {
    operator fun invoke(region: Region): Filter = Filter { next ->
        { request -> next(request.uri(request.uri.host("lambda.${region.name}.amazonaws.com").scheme("https"))) }
    }
}

internal interface AwsClientHttpAdapter<Req, Resp> {
    operator fun invoke(response: Resp): Response
    operator fun invoke(request: Request): Req

    val requestLens: BiDiBodyLens<Req>
    val responseLens: BiDiBodyLens<Resp>
}

class AwsClientV1HttpAdapter :
    AwsClientHttpAdapter<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    override fun invoke(response: APIGatewayProxyResponseEvent) = Response(Status(response.statusCode, ""))
        .headers(response.headers.map { kv -> kv.toPair() })
        .body(response.body)

    override fun invoke(request: Request) = APIGatewayProxyRequestEvent()
        .withHttpMethod(request.method.name)
        .withHeaders(request.headers.toMap())
        .withPath(request.uri.path)
        .withQueryStringParameters(request.uri.query.toParameters().toMap())
        .withBody(request.bodyString())

    override val requestLens = Body.auto<APIGatewayProxyRequestEvent>().toLens()
    override val responseLens = Body.auto<APIGatewayProxyResponseEvent>().toLens()
}
