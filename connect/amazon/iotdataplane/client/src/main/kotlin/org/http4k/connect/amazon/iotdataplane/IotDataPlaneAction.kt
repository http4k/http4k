package org.http4k.connect.amazon.iotdataplane

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface IotDataPlaneAction<R> : Action<Result<R, RemoteFailure>>
