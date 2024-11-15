package org.http4k.connect.plugin.bar

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface BarAction<R> : Action<Result<R, RemoteFailure>>
