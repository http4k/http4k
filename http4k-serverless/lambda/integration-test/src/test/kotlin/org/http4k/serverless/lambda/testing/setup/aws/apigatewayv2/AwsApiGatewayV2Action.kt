package org.http4k.serverless.lambda.testing.setup.aws.apigatewayv2

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Response
import org.http4k.serverless.lambda.testing.setup.aws.Action
import org.http4k.serverless.lambda.testing.setup.aws.RemoteFailure
import kotlin.reflect.KClass

abstract class AwsApiGatewayV2Action<R : Any>(private val clazz: KClass<R>) : Action<Result<R, RemoteFailure>> {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(ApiGatewayJackson.asA(bodyString().let { if (it.isEmpty()) "{}" else it }, clazz))
            else -> Failure(RemoteFailure(toRequest().method, toRequest().uri, status, bodyString()))
        }
    }
}
