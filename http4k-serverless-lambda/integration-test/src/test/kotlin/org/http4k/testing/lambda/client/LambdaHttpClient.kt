package org.http4k.testing.lambda.client

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.toParameters
import org.http4k.core.with
import org.http4k.format.Jackson.auto

object LambdaHttpClient {
    operator fun invoke(functionName: FunctionName, region: Region): Filter =
        callFunction(functionName).then(LambdaApi(region))

    private fun callFunction(functionName: FunctionName): Filter {
        return Filter { next ->
            {
                val lambdaResponse = next(Request(Method.POST, "/2015-03-31/functions/${functionName.value}/invocations")
                    .header("X-Amz-Invocation-Type", "RequestResponse")
                    .header("X-Amz-Log-Type", "Tail")
                    .with(lambdaRequest of APIGatewayProxyRequestEvent()
                        .withHttpMethod(it.method.name)
                        .withPath(it.uri.path)
                        .withQueryStringParameters(it.uri.query.toParameters().toMap())
                        .withBody(it.bodyString())))

                Response(Status.OK).body(lambdaResponse.body)
            }
        }
    }

    private val lambdaRequest = Body.auto<APIGatewayProxyRequestEvent>().toLens()
}

object LambdaApi {
    operator fun invoke(region: Region): Filter = Filter { next ->
        { request -> next(request.uri(request.uri.host("lambda.${region.name}.amazonaws.com").scheme("https"))) }
    }
}