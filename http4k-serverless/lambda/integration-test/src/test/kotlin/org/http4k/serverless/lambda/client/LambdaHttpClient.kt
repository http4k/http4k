package org.http4k.serverless.lambda.client

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent
import org.http4k.aws.FunctionName
import org.http4k.aws.LambdaIntegrationType
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV1
import org.http4k.aws.LambdaIntegrationType.ApiGatewayV2
import org.http4k.aws.LambdaIntegrationType.ApplicationLoadBalancer
import org.http4k.aws.Region
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.queries
import org.http4k.core.then
import org.http4k.core.toParameters
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.lens.BiDiBodyLens

class LambdaHttpClient(functionName: FunctionName, region: Region, version: LambdaIntegrationType) : Filter {
    private val adapter = when (version) {
        ApiGatewayV1 -> AwsClientV1HttpAdapter()
        ApiGatewayV2 -> AwsClientV2HttpAdapter()
        ApplicationLoadBalancer -> AwsClientAlbHttpAdapter()
    }

    private fun callFunction(functionName: FunctionName) = Filter { next ->
        HttpHandler {
            val request: Request = Request(Method.POST, "/2015-03-31/functions/${functionName.value}/invocations")
                .header("X-Amz-Invocation-Type", "RequestResponse")
                .header("X-Amz-Log-Type", "Tail")
                .with(adapter.inject(it))

            val lambdaResponse = next(request)

            adapter.extract(lambdaResponse)
        }
    }

    private val filter = callFunction(functionName).then(LambdaApi(region))

    override fun invoke(handler: HttpHandler): HttpHandler = filter(handler)
}

object LambdaApi {
    operator fun invoke(region: Region): Filter = Filter { next ->
        HttpHandler { request -> next(request.uri(request.uri.host("lambda.${region.name}.amazonaws.com").scheme("https"))) }
    }
}

internal interface AwsClientHttpAdapter<Req, Resp> {
    operator fun invoke(response: Resp): Response
    operator fun invoke(request: Request): Req

    fun inject(request: Request): (Request) -> Request = requestLens of this(request)
    fun extract(response: Response): Response = this(responseLens(response))

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

internal class AwsClientV2HttpAdapter : AwsClientHttpAdapter<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
    override fun invoke(response: APIGatewayV2HTTPResponse) =
        Response(Status(response.statusCode, ""))
            .headers((response.headers
                ?: emptyMap()).entries.fold(listOf(), { acc, next -> acc + (next.key to next.value) }))
            .headers((response.multiValueHeaders ?: emptyMap()).entries.fold(listOf(), { acc, next ->
                next.value.fold(acc, { acc2, next2 -> acc2 + (next.key to next2) })
            }))
            .body(response.body.orEmpty())


    override fun invoke(request: Request) = APIGatewayV2HTTPEvent.builder()
        .withRawPath(request.uri.path)
        .withQueryStringParameters(request.uri.queries().toMap())
        .withBody(request.bodyString())
        .withHeaders(request.headers.toMap())
        .withRequestContext(APIGatewayV2HTTPEvent.RequestContext.builder()
            .withHttp(
                APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod(request.method.name).build()
            ).build()
        )
        .build()

    override val requestLens = Body.auto<APIGatewayV2HTTPEvent>().toLens()
    override val responseLens = Body.auto<APIGatewayV2HTTPResponse>().toLens()
}

internal class AwsClientAlbHttpAdapter : AwsClientHttpAdapter<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {
    override fun invoke(response: ApplicationLoadBalancerResponseEvent) =
        Response(Status(response.statusCode, ""))
            .headers((response.headers
                ?: emptyMap()).entries.fold(listOf(), { acc, next -> acc + (next.key to next.value) }))
            .headers((response.multiValueHeaders ?: emptyMap()).entries.fold(listOf(), { acc, next ->
                next.value.fold(acc, { acc2, next2 -> acc2 + (next.key to next2) })
            }))
            .body(response.body.orEmpty())


    override fun invoke(request: Request) = ApplicationLoadBalancerRequestEvent().apply {
        httpMethod = request.method.name
        body = request.bodyString()
        headers = request.headers.toMap()
        path = request.uri.path
        queryStringParameters = request.uri.query.toParameters().toMap()
    }

    override val requestLens = Body.auto<ApplicationLoadBalancerRequestEvent>().toLens()
    override val responseLens = Body.auto<ApplicationLoadBalancerResponseEvent>().toLens()
}

