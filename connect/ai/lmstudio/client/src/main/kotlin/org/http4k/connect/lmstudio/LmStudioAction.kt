package org.http4k.connect.lmstudio

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface LmStudioAction<R> : Action<Result<R, RemoteFailure>>
