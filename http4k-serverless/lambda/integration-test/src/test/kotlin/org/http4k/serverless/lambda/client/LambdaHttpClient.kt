package org.http4k.serverless.lambda.client

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent
import org.http4k.aws.FunctionName
import org.http4k.aws.Region
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.queries
import org.http4k.core.then
import org.http4k.core.toParameters
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.string
import org.http4k.serverless.aws.AwsGatewayProxyRequestV2
import org.http4k.serverless.aws.Http
import org.http4k.serverless.aws.RequestContext

abstract class LambdaHttpClient<Req, Resp>(functionName: FunctionName, region: Region) : Filter {
    private fun callFunction(functionName: FunctionName) = Filter { next ->
        {
            extract(next(Request(POST, "/2015-03-31/functions/${functionName.value}/invocations")
                .header("X-Amz-Invocation-Type", "RequestResponse")
                .header("X-Amz-Log-Type", "Tail")
                .with(inject(it))))
        }
    }

    protected abstract fun inject(it: Request): (Request) -> Request

    protected abstract fun extract(lambdaResponse: Response): Response

    private val filter = callFunction(functionName).then(LambdaApi(region))

    override fun invoke(handler: HttpHandler): HttpHandler = filter(handler)
}

class ApiGatewayV1LambdaClient(functionName: FunctionName, region: Region) :
    LambdaHttpClient<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>(functionName, region) {
    override fun inject(it: Request) = AwsClientV1HttpAdapter.inject(it)

    override fun extract(lambdaResponse: Response) = AwsClientV1HttpAdapter.extract(lambdaResponse)
}

class ApiGatewayV2LambdaClient(functionName: FunctionName, region: Region) :
    LambdaHttpClient<AwsGatewayProxyRequestV2, APIGatewayV2HTTPResponse>(functionName, region) {
    override fun inject(it: Request) = AwsClientV2HttpAdapter.inject(it)

    override fun extract(lambdaResponse: Response) = AwsClientV2HttpAdapter.extract(lambdaResponse)
}

class ApplicationLoadBalancerLambdaClient(functionName: FunctionName, region: Region) :
    LambdaHttpClient<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent>(functionName, region) {
    override fun inject(it: Request) = AwsClientAlbHttpAdapter.inject(it)

    override fun extract(lambdaResponse: Response) = AwsClientAlbHttpAdapter.extract(lambdaResponse)
}

class InvocationLambdaClient(functionName: FunctionName, region: Region) :
    LambdaHttpClient<String, String>(functionName, region) {

    override fun inject(it: Request) = AwsClientInvocationHttpAdapter.inject(it)

    override fun extract(lambdaResponse: Response) = AwsClientInvocationHttpAdapter.extract(lambdaResponse)
}

object LambdaApi {
    operator fun invoke(region: Region): Filter = Filter { next ->
        { request -> next(request.uri(request.uri.host("lambda.${region.name}.amazonaws.com").scheme("https"))) }
    }
}

interface AwsClientHttpAdapter<Req, Resp> {
    operator fun invoke(response: Resp): Response
    operator fun invoke(request: Request): Req

    fun inject(request: Request): (Request) -> Request = requestLens of this(request)
    fun extract(response: Response): Response = this(responseLens(response))

    val requestLens: BiDiBodyLens<Req>
    val responseLens: BiDiBodyLens<Resp>
}

internal object AwsClientV1HttpAdapter :
    AwsClientHttpAdapter<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    override operator fun invoke(response: APIGatewayProxyResponseEvent) = Response(Status(response.statusCode, ""))
        .headers(response.headers.map { kv -> kv.toPair() })
        .body(response.body)

    override operator fun invoke(request: Request) = APIGatewayProxyRequestEvent()
        .withHttpMethod(request.method.name)
        .withHeaders(request.headers.toMap())
        .withPath(request.uri.path)
        .withQueryStringParameters(request.uri.query.toParameters().toMap())
        .withBody(request.bodyString())

    override val requestLens = Body.auto<APIGatewayProxyRequestEvent>().toLens()
    override val responseLens = Body.auto<APIGatewayProxyResponseEvent>().toLens()
}

internal object AwsClientV2HttpAdapter : AwsClientHttpAdapter<AwsGatewayProxyRequestV2, APIGatewayV2HTTPResponse> {
    override operator fun invoke(response: APIGatewayV2HTTPResponse) =
        Response(Status(response.statusCode, ""))
            .headers((response.headers
                ?: emptyMap()).entries.fold(listOf(), { acc, next -> acc + (next.key to next.value) }))
            .headers((response.multiValueHeaders ?: emptyMap()).entries.fold(listOf(), { acc, next ->
                next.value.fold(acc, { acc2, next2 -> acc2 + (next.key to next2) })
            }))
            .headers((response.cookies ?: emptyList()).fold(listOf(), { acc, next -> acc + ("set-cookie" to next) }))
            .body(response.body.orEmpty())


    override fun invoke(request: Request) = AwsGatewayProxyRequestV2(requestContext = RequestContext(Http(request.method.name))).apply {
        rawPath = request.uri.path
        queryStringParameters = request.uri.queries().filterNot { it.second == null }.map { it.first to it.second!! }.toMap()
        body = request.bodyString()
        headers = request.headers.groupBy { it.first }.mapValues { (k, v) -> v.map { it.second }.joinToString(",") }
    }

    override val requestLens = Body.auto<AwsGatewayProxyRequestV2>().toLens()
    override val responseLens = Body.auto<APIGatewayV2HTTPResponse>().toLens()
}

internal object AwsClientAlbHttpAdapter : AwsClientHttpAdapter<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {
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

internal object AwsClientInvocationHttpAdapter : AwsClientHttpAdapter<String, String> {
    override fun invoke(response: String) = Response(OK).body(response)
    override fun invoke(request: Request) = request.bodyString()
    override val requestLens = Body.string(TEXT_PLAIN).toLens()
    override val responseLens = Body.string(TEXT_PLAIN).toLens()
}

