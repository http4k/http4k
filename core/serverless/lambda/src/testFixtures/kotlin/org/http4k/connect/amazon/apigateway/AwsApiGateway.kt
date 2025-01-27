package org.http4k.connect.amazon.apigateway

import dev.forkhandles.result4k.Result
import org.http4k.connect.RemoteFailure

interface AwsApiGateway {
    operator fun <R : Any> invoke(action: AwsApiGatewayAction<R>): Result<R, RemoteFailure>

    companion object
}

