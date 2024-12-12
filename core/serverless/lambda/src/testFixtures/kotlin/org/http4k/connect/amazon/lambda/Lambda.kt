package org.http4k.connect.amazon.lambda

import dev.forkhandles.result4k.Result
import org.http4k.connect.RemoteFailure

interface Lambda {
    operator fun <R : Any> invoke(action: LambdaAction<R>): Result<R, RemoteFailure>

    companion object
}
