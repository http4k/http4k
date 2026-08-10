package org.http4k.connect.amazon.iot

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface IotAction<R> : Action<Result<R, RemoteFailure>>
