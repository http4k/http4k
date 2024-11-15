package org.http4k.connect.github.api

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface GitHubAction<R> : Action<Result<R, RemoteFailure>>

