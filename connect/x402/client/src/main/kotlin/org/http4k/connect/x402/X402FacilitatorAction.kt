package org.http4k.connect.x402

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface X402FacilitatorAction<R> : Action<Result<R, RemoteFailure>>
