package org.http4k.connect.amazon.xray

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface XRayAction<R> : Action<Result<R, RemoteFailure>>
