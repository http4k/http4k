package org.http4k.serverless.lambda.client

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.aws.FunctionName
import org.http4k.aws.Region
import org.http4k.aws.Role
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
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

    private fun callFunction(functionName: FunctionName) = Filter { next ->
        {
            val lambdaResponse = next(
                Request(Method.POST, "/2015-03-31/functions/${functionName.value}/invocations")
                    .header("X-Amz-Invocation-Type", "RequestResponse")
                    .header("X-Amz-Log-Type", "Tail")
                    .with(
                        lambdaRequest of APIGatewayProxyRequestEvent()
                            .withHttpMethod(it.method.name)
                            .withHeaders(it.headers.toMap())
                            .withPath(it.uri.path)
                            .withQueryStringParameters(it.uri.query.toParameters().toMap())
                            .withBody(it.bodyString())
                    )
            )

            val response = lambdaResponse(lambdaResponse)

            Response(Status(response.statusCode, ""))
                .headers(response.headers.map { kv -> kv.toPair() })
                .body(response.body)
        }
    }

    private val lambdaRequest = Body.auto<APIGatewayProxyRequestEvent>().toLens()
    private val lambdaResponse = Body.auto<APIGatewayProxyResponseEvent>().toLens()
}

object LambdaApi {
    operator fun invoke(region: Region): Filter = Filter { next ->
        { request -> next(request.uri(request.uri.host("lambda.${region.name}.amazonaws.com").scheme("https"))) }
    }
}

