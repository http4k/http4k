package org.http4k.aws.lambda

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.toUrlEncoded

/**
 * This is the main entry point for the lambda. It uses the local environment
 * to instantiate the Http4k handler which can be used for further invocations.
 */
class LambdaFunction(private val env: Map<String, String> = System.getenv()) {
    private val app = BootstrapAppLoader(env)

    fun handle(request: ApiGatewayProxyRequest) = app(request.asHttp4k()).asApiGateway()
}

internal fun Response.asApiGateway() = ApiGatewayProxyResponse(status.code, headers.toMap(), bodyString())

internal fun ApiGatewayProxyRequest.asHttp4k() = headers.toList().fold(
    Request(Method.valueOf(httpMethod), uri())
        .body(body?.let(::Body) ?: Body.EMPTY)) { memo, (first, second) ->
    memo.header(first, second)
}

internal fun ApiGatewayProxyRequest.uri() = Uri.of(path).query(queryStringParameters.toList().toUrlEncoded())
