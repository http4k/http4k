package org.http4k.connect.amazon.sts

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface STSAction<R> : Action<Result<R, RemoteFailure>>
