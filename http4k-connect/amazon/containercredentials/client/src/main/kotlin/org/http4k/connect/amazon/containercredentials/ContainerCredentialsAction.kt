package org.http4k.connect.amazon.containercredentials

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface ContainerCredentialsAction<R> : Action<Result<R, RemoteFailure>>
