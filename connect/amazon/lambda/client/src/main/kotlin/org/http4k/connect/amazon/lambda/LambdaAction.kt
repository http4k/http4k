package org.http4k.connect.amazon.lambda

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface LambdaAction<R> : Action<Result<R, RemoteFailure>>
