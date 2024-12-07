package org.http4k.connect.amazon.apigatewayv2

import dev.forkhandles.result4k.Result
import org.http4k.connect.RemoteFailure

interface AwsApiGatewayV2 {
    operator fun <R : Any> invoke(action: AwsApiGatewayV2Action<R>): Result<R, RemoteFailure>

    companion object
}

